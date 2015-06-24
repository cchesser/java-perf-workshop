package cchesser.javaperf.workshop.data;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link cchesser.javaperf.workshop.data.ConferenceSessionLoader}.
 */
public class ConferenceSessionLoaderTest {

    @Test
    public void testGetContent() {
        List<ConferenceSession> sessions = new ConferenceSessionLoader().load();
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
