package cchesser.javaperf.workshop.data;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link cchesser.javaperf.workshop.data.ConferenceSessionLoader}.
 */
public class ConferenceSessionLoaderTest extends BaseTest {

    @Test
    public void testGetContent() {
        setupMock();
        List<ConferenceSession> sessions = new ConferenceSessionLoader(getConfiguration()).load();
        assertTrue(sessions.size() > 0);
        for(ConferenceSession session : sessions) {
            assertNotNull(session.getTitle());
            assertNotNull(session.getAbstract());
            assertNotNull(session.getPresenter());
            assertNotNull(session.getPresenter().getName());
            if (session.getTags() != null) {
                assertTrue(session.getTags().size() > 0);
            }
        }
    }
}
