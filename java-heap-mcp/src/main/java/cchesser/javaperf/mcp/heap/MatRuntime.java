package cchesser.javaperf.mcp.heap;

import org.eclipse.core.internal.registry.ExtensionRegistry;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.RegistryFactory;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.dynamichelpers.ExtensionTracker;
import org.eclipse.core.runtime.dynamichelpers.IExtensionTracker;
import org.eclipse.core.runtime.spi.RegistryContributor;
import org.eclipse.core.runtime.spi.RegistryStrategy;
import org.eclipse.mat.internal.MATPlugin;
import org.eclipse.mat.parser.internal.ParserPlugin;
import org.eclipse.mat.report.internal.ReportPlugin;
import org.eclipse.mat.hprof.HprofPlugin;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.core.runtime.content.IContentTypeManager;
import org.osgi.util.tracker.ServiceTracker;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.jar.JarFile;

/** Initializes the MAT OSGi services needed when MAT is used from a plain JVM. */
final class MatRuntime {
    private static final Object LOCK = new Object();
    private static volatile boolean initialized;

    private MatRuntime() {
    }

    static void initialize() {
        if (initialized) {
            return;
        }
        synchronized (LOCK) {
            if (initialized) {
                return;
            }
            try {
                IExtensionRegistry registry = RegistryFactory.createRegistry(new ClassLoadingRegistryStrategy(), null, null);
                RegistryFactory.setDefaultRegistryProvider(() -> registry);
                addMatContributions((ExtensionRegistry) registry);

                installContentTypeManager(simpleContentTypes());
                activateHprofPlugin();

                IExtensionTracker tracker = new ExtensionTracker(registry);
                MATPlugin matPlugin = new MATPlugin();
                setBundle(matPlugin, bundle("org.eclipse.mat.api"));
                activate(MATPlugin.class, matPlugin, tracker);
                ReportPlugin reportPlugin = new ReportPlugin();
                setBundle(reportPlugin, bundle("org.eclipse.mat.report"));
                activate(ReportPlugin.class, reportPlugin, tracker);
                ParserPlugin parserPlugin = new ParserPlugin();
                setStaticField(ParserPlugin.class, "plugin", parserPlugin);
                setField(parserPlugin, "tracker", tracker);
                setField(parserPlugin, "registry", new org.eclipse.mat.parser.internal.util.ParserRegistry(tracker));
                initialized = true;
            } catch (ReflectiveOperationException | IOException | CoreException exception) {
                throw new IllegalStateException("Unable to initialize the MAT runtime", exception);
            }
        }
    }

    private static void addMatContributions(ExtensionRegistry registry) throws IOException {
        List<String> bundles = List.of(
                "org.eclipse.mat.api.jar",
                "org.eclipse.mat.report.jar",
                "org.eclipse.mat.parser.jar",
                "org.eclipse.mat.hprof.jar"
        );
        Path matDirectory = findMatDirectory();
        for (String bundle : bundles) {
            Path path = findBundle(matDirectory, bundle);
            try (JarFile jar = new JarFile(path.toFile()); InputStream pluginXml = jar.getInputStream(jar.getJarEntry("plugin.xml"))) {
                String contributor = bundle.substring(0, bundle.length() - ".jar".length());
                RegistryContributor registryContributor = new RegistryContributor(contributor, contributor, null, null);
                registry.addContribution(pluginXml, registryContributor, true, contributor, null, null);
            }
        }
    }

    private static Path findMatDirectory() throws IOException {
        List<Path> candidates = new ArrayList<>();
        String configuredDirectory = System.getProperty("java.heap.mcp.mat.directory");
        if (configuredDirectory != null && !configuredDirectory.isBlank()) {
            candidates.add(Path.of(configuredDirectory));
        }
        candidates.add(Path.of("target", "mat"));
        candidates.add(Path.of("vendor", "mat"));

        try {
            Path applicationDirectory = Path.of(MatRuntime.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI()).getParent();
            candidates.add(applicationDirectory.resolve("mat"));
            candidates.add(applicationDirectory.resolveSibling("vendor").resolve("mat"));
        } catch (Exception ignored) {
            // The working-directory candidates still cover normal Maven/script launches.
        }

        for (Path candidate : candidates) {
            if (Files.isDirectory(candidate) && hasMatBundle(candidate)) {
                return candidate;
            }
        }
        throw new IOException("Could not find MAT bundles. Checked: " + candidates);
    }

    private static boolean hasMatBundle(Path directory) {
        return Files.exists(directory.resolve("org.eclipse.mat.api.jar"))
                || hasVersionedBundle(directory, "org.eclipse.mat.api.jar");
    }

    private static Path findBundle(Path directory, String bundleName) throws IOException {
        Path exactPath = directory.resolve(bundleName);
        if (Files.isRegularFile(exactPath)) {
            return exactPath;
        }
        try (var paths = Files.list(directory)) {
            return paths.filter(path -> path.getFileName().toString().startsWith(bundleName.substring(0, bundleName.length() - 4) + "_")
                            && path.getFileName().toString().endsWith(".jar"))
                    .findFirst()
                    .orElseThrow(() -> new IOException("Could not find MAT bundle " + bundleName + " in " + directory));
        }
    }

    private static boolean hasVersionedBundle(Path directory, String bundleName) {
        try (var paths = Files.list(directory)) {
            String prefix = bundleName.substring(0, bundleName.length() - 4) + "_";
            return paths.anyMatch(path -> path.getFileName().toString().startsWith(prefix)
                    && path.getFileName().toString().endsWith(".jar"));
        } catch (IOException exception) {
            return false;
        }
    }

    private static void activate(Class<?> pluginType, Object plugin, IExtensionTracker tracker)
            throws ReflectiveOperationException {
        setStaticField(pluginType, "plugin", plugin);
        setField(plugin, "tracker", tracker);
    }

    private static void installContentTypeManager(Object manager) throws ReflectiveOperationException {
        Class<?> platformType = Class.forName("org.eclipse.core.internal.runtime.InternalPlatform");
        Object platform = platformType.getMethod("getDefault").invoke(null);
        Field trackerField = platformType.getDeclaredField("contentTracker");
        trackerField.setAccessible(true);
        trackerField.set(platform, new ContentTypeServiceTracker(manager));
        Field preferenceTracker = platformType.getDeclaredField("preferencesTracker");
        preferenceTracker.setAccessible(true);
        IPreferencesService preferences = (IPreferencesService) Proxy.newProxyInstance(
                MatRuntime.class.getClassLoader(), new Class<?>[]{IPreferencesService.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getString" -> "";
                    case "getBoolean" -> false;
                    default -> null;
                });
        preferenceTracker.set(platform, new ContentTypeServiceTracker(preferences));
    }

    private static void activateHprofPlugin() throws ReflectiveOperationException {
        Class<?> platformActivator = Class.forName("org.eclipse.core.internal.runtime.PlatformActivator");
        Field context = platformActivator.getDeclaredField("context");
        context.setAccessible(true);
        context.set(null, ContentTypeServiceTracker.nullContext());
        HprofPlugin plugin = new HprofPlugin();
        setBundle(plugin, bundle("org.eclipse.mat.hprof"));
        setStaticField(HprofPlugin.class, "plugin", plugin);
    }

    private static Bundle bundle(String symbolicName) {
        return (Bundle) Proxy.newProxyInstance(
                MatRuntime.class.getClassLoader(), new Class<?>[]{Bundle.class},
                (proxy, method, args) -> {
                    if ("getSymbolicName".equals(method.getName())) {
                        return symbolicName;
                    }
                    if ("getEntry".equals(method.getName()) && args != null && args.length == 1 && args[0] != null) {
                        String resource = args[0].toString().replaceFirst("^\\$nl\\$/?", "").replaceFirst("^/", "");
                        return MatRuntime.class.getClassLoader().getResource(resource);
                    }
                    return null;
                });
    }

    private static void setBundle(Object plugin, Bundle bundle) throws ReflectiveOperationException {
        Field bundleField = org.eclipse.core.runtime.Plugin.class.getDeclaredField("bundle");
        bundleField.setAccessible(true);
        bundleField.set(plugin, bundle);
    }

    private static IContentTypeManager simpleContentTypes() {
        IContentType base = contentType("org.eclipse.mat.JavaHeapDump", null);
        IContentType hprof = contentType("org.eclipse.mat.HprofHeapDump", base);
        return (IContentTypeManager) Proxy.newProxyInstance(
                MatRuntime.class.getClassLoader(), new Class<?>[]{IContentTypeManager.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getContentType" -> "org.eclipse.mat.HprofHeapDump".equals(args[0]) ? hprof : base;
                    case "findContentTypeFor" -> hprof;
                    case "findContentTypesFor" -> new IContentType[]{hprof, base};
                    case "getAllContentTypes" -> new IContentType[]{base, hprof};
                    case "getMatcher" -> proxy;
                    default -> null;
                });
    }

    private static IContentType contentType(String id, IContentType base) {
        return (IContentType) Proxy.newProxyInstance(
                MatRuntime.class.getClassLoader(), new Class<?>[]{IContentType.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getId" -> id;
                    case "getName" -> id;
                    case "getBaseType" -> base;
                    case "isKindOf" -> args[0] == base || proxy == args[0] || id.equals(((IContentType) args[0]).getId());
                    case "getFileSpecs" -> new String[0];
                    case "getDefaultCharset" -> "UTF-8";
                    case "isUserDefined", "isAssociatedWith" -> false;
                    default -> null;
                });
    }

    private static void setStaticField(Class<?> type, String name, Object value) throws ReflectiveOperationException {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        field.set(null, value);
    }

    private static void setField(Object target, String name, Object value) throws ReflectiveOperationException {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static final class ClassLoadingRegistryStrategy extends RegistryStrategy {
        private ClassLoadingRegistryStrategy() {
            super(new java.io.File[]{Path.of(System.getProperty("java.io.tmpdir"), "java-heap-mcp-mat-registry").toFile()},
                    new boolean[]{false});
        }

        @Override
        public Object createExecutableExtension(RegistryContributor contributor, String className,
                                                 String overriddenContributorName) throws CoreException {
            try {
                return Class.forName(className).getDeclaredConstructor().newInstance();
            } catch (ReflectiveOperationException | LinkageError exception) {
                throw new CoreException(new Status(IStatus.ERROR, "java-heap-mcp", exception.getMessage(), exception));
            }
        }
    }

    private static final class ContentTypeServiceTracker extends ServiceTracker<Object, Object> {
        private final Object service;

        private ContentTypeServiceTracker(Object service) {
            super(nullContext(), "org.eclipse.core.runtime.content.IContentTypeManager", null);
            this.service = Objects.requireNonNull(service);
        }

        @Override
        public Object getService() {
            return service;
        }

        private static BundleContext nullContext() {
            return (BundleContext) Proxy.newProxyInstance(
                    MatRuntime.class.getClassLoader(),
                    new Class<?>[]{BundleContext.class},
                    (proxy, method, args) -> null
            );
        }
    }

}
