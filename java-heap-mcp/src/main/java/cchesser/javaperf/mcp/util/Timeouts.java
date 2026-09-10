package cchesser.javaperf.mcp.util;

import cchesser.javaperf.mcp.heap.HeapOperationException;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class Timeouts {
    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool(r -> {
        Thread thread = new Thread(r, "java-heap-mcp-worker");
        thread.setDaemon(true);
        return thread;
    });

    private Timeouts() {
    }

    public static <T> T call(String operationName, Duration timeout, Callable<T> task) {
        Future<T> future = EXECUTOR.submit(task);
        try {
            return future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException exception) {
            future.cancel(true);
            throw HeapOperationException.timeout(operationName, timeout, exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw HeapOperationException.interrupted(operationName, exception);
        } catch (ExecutionException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new RuntimeException(cause);
        }
    }
}
