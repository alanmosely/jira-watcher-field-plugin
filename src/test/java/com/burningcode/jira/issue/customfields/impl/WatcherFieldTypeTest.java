import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.mockito.Mockito;

import com.atlassian.jira.issue.comparator.UserComparator;
import com.atlassian.jira.user.ApplicationUser;

public class WatcherFieldTypeTest {

    @Test
    public void testValuesEqualIgnoresOrder() {
        WatcherFieldType type = Mockito.mock(WatcherFieldType.class, Mockito.CALLS_REAL_METHODS);

        ApplicationUser user1 = Mockito.mock(ApplicationUser.class);
        Mockito.when(user1.getName()).thenReturn("a");
        ApplicationUser user2 = Mockito.mock(ApplicationUser.class);
        Mockito.when(user2.getName()).thenReturn("b");

        Collection<ApplicationUser> list1 = Arrays.asList(user1, user2);
        Collection<ApplicationUser> list2 = Arrays.asList(user2, user1);

        assertTrue(type.valuesEqual(list1, list2));
    }
}
