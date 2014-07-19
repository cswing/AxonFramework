package org.axonframework.saga.annotation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.mockito.Mockito.spy;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import org.axonframework.domain.GenericEventMessage;
import org.axonframework.eventhandling.SimpleEventBus;
import org.axonframework.saga.AssociationValue;
import org.axonframework.saga.Saga;
import org.axonframework.saga.repository.inmemory.InMemorySagaRepository;
import org.junit.Before;
import org.junit.Test;

/**
 * The scenario being described and tested here is that an event should start two separate sagas, one for 
 * the from and one for the to.  Before this test and subsequent code changes, only one 
 * of the methods would be called.  The root cause was that the {@link SagaMethodMessageHandlerInspector}
 * uses a {@link TreeSet}.  The side effect of using a TreeSet is that the {@link Comparable#compareTo(Object)}
 * method is used for comparison.
 * 
 * The {@link SagaMethodMessageHandler#compareTo(SagaMethodMessageHandler)} method should take into account the 
 * property that is being associated, otherwise distinct methods could be considered equal.
 * 
 * from http://docs.oracle.com/javase/7/docs/api/java/util/TreeSet.html
 * <blockquote>
 * Note that the ordering maintained by a set (whether or not an explicit comparator is provided) must be consistent with equals if it is to 
 * correctly implement the Set interface. (See Comparable or Comparator for a precise definition of consistent with equals.) This is so because 
 * the Set interface is defined in terms of the equals operation, but a TreeSet instance performs all element comparisons using its compareTo 
 * (or compare) method, so two elements that are deemed equal by this method are, from the standpoint of the set, equal. The behavior of a set is 
 * well-defined even if its ordering is inconsistent with equals; it just fails to obey the general contract of the Set interface.
 * </blockquote>
 * 
 * @author Craig Swing
 *
 */
public class AnnotatedSagaManagerMultipleSagaTest {
	
	private InMemorySagaRepository sagaRepository;
    private AnnotatedSagaManager manager;

    @Before
    public void setUp() throws Exception {
        sagaRepository = spy(new InMemorySagaRepository());
        manager = new AnnotatedSagaManager(sagaRepository, new SimpleEventBus(), MultipleTestSaga.class);
    }

    @Test
    public void testCreateMultipleSagasFromSingleEvent() {
    	
    	manager.handle(new GenericEventMessage<TestInitiationEvent>(new TestInitiationEvent("from123", "to123")));
        
    	Set<MultipleTestSaga> froms = repositoryContents("from123", MultipleTestSaga.class); 
    	Set<MultipleTestSaga> tos = repositoryContents("to123", MultipleTestSaga.class);
    	
    	MultipleTestSaga from = froms.iterator().next();
    	MultipleTestSaga to = tos.iterator().next();
    	
    	assertEquals(1, froms.size());
    	assertEquals(1, tos.size());
    	assertNotEquals(from, to);
    }
    
    private <T extends Saga> Set<T> repositoryContents(String lookupValue, Class<T> sagaType) {
        final Set<String> identifiers = sagaRepository.find(sagaType, new AssociationValue("myIdentifier",
                                                                                           lookupValue));
        Set<T> sagas = new HashSet<T>();
        for (String identifier : identifiers) {
            sagas.add((T) sagaRepository.load(identifier));
        }
        return sagas;
    }
	
	public static class MultipleTestSaga extends AbstractAnnotatedSaga {
		
		@StartSaga
		@SagaEventHandler(keyName="myIdentifier", associationProperty="from")
		public void onInitateForFrom(TestInitiationEvent event) {}
		
		@StartSaga
		@SagaEventHandler(keyName="myIdentifier", associationProperty="to")
		public void onInitateForTo(TestInitiationEvent event) {}
	}
	
	public static class TestInitiationEvent {
		
		private String from;
		
		private String to;

		public TestInitiationEvent(String from, String to) {
			this.from = from;
			this.to = to;
		}

		public String getFrom() {
			return from;
		}

		public String getTo() {
			return to;
		}
	}
}
