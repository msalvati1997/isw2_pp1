package isw2_pp1;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.junit.BeforeClass;
import org.junit.Test;
import org.apache.jcs.engine.CacheElement;
import org.apache.jcs.engine.CacheEventQueue;
import org.apache.log4j.BasicConfigurator;


/**
 * This test case is designed to makes sure there are no deadlocks in the event
 * queue. The time to live should be set to a very short interval to make a
 * deadlock more likely.

 */
@RunWith(value=Parameterized.class)
public class EventQueueConcurrentLoad_ParametrizedTest  {
	
		private static CacheEventQueue queue = null;
	    private static CacheListenerImpl listen = null;
	    private static int maxFailure = 3;
	    private static int waitBeforeRetry = 100;
	    // very small idle time
	    private static int idleTime = 2;
		private int end;
		private int expectedPutCount;
		private String testname;
		
	    public EventQueueConcurrentLoad_ParametrizedTest(String testname,int end, int expectedPutCount) {
			this.end=end;
			this.expectedPutCount=expectedPutCount;
			this.testname= testname;
		}
	    @Parameters()
        public static List<Object[]> paramList() {
            return Arrays.asList(new Object[][]{
                    {"runPutTest",200,200},
                    {"runPutTest",1200,1400},
                    {"RunRemoveTest",2200,0},
                    {"StopProcessing",0,0},
                    {"runPutTest",5200,6600},
                    {"RunRemoveTest",5200,0},
                    {"StopProcessing",0,0},
                    {"RunPutDelayTest",100,6700},
            });
        }
        
        @BeforeClass
        /**
         * Test Configuration. Create the static queue to be used by all tests
         */
        public static void configure()
        {
	    BasicConfigurator.configure();
            listen = new CacheListenerImpl();
            queue = new CacheEventQueue( listen, 1L, "testCache1", maxFailure, waitBeforeRetry );
            queue.setWaitToDieMillis( idleTime );
        }
@Test(timeout=7000)
/**
 * Testing the queue.
 *
 * @throws Exception
 */
public void ChoosingTest() throws Exception {
	if (testname=="runPutTest") {
	     Thread.sleep( 250 );
		runPutTest(end,expectedPutCount);
	}
    if (testname=="RunRemoveTest") {
        Thread.sleep( 250 );
    	runRemoveTest(end);
	}
    if (testname=="StopProcessing") {
        Thread.sleep( 250 );
        runStopProcessingTest();
    }
    if (testname=="RunPutDelayTest") {
        Thread.sleep( 250 );
    	runPutDelayTest(end,expectedPutCount);
    }
}
/**
 * Adds put events to the queue.
 *
 * @param end
 * @param expectedPutCount
 * @throws Exception
 */
public void runPutTest( int end, int expectedPutCount )throws Exception {
    for ( int i = 0; i <= end; i++ )
    {
        CacheElement elem = new CacheElement( "testCache1", i + ":key", i + "data" );
        queue.addPutEvent( elem );
    }

    while ( !queue.isEmpty() )
    {
        synchronized ( this )
        {
            System.out.println( "queue is still busy, waiting 250 millis" );
            this.wait( 250 );
        }
    }
    System.out.println( "queue is empty, comparing putCount" );

    // this becomes less accurate with each test. It should never fail. If
    // it does things are very off.
    assertTrue( "The put count [" + listen.putCount + "] is below the expected minimum threshold ["
        + expectedPutCount + "]", listen.putCount >= ( expectedPutCount - 1 ) );

}

/**
 * Add remove events to the event queue.
 *
 * @param end
 * @throws Exception
 */
public void runRemoveTest(int end)
    throws Exception
{
    for ( int i = 0; i <= end; i++ )
    {
        queue.addRemoveEvent( i + ":key" );
    }
}

/**
 * Add remove events to the event queue.
 *
 * @throws Exception
 */
public void runStopProcessingTest()
    throws Exception
{
    queue.stopProcessing();
}

/**
 * Test putting and a delay. Waits until queue is empty to start.
 *
 * @param end
 * @param expectedPutCount
 * @throws Exception
 */
public void runPutDelayTest( int end, int expectedPutCount ) throws Exception
{
    while ( !queue.isEmpty() )
    {
        synchronized ( this )
        {
            System.out.println( "queue is busy, waiting 250 millis to begin" );
            this.wait( 250 );
        }
    }
    System.out.println( "queue is empty, begin" );

    // get it going
    CacheElement elem = new CacheElement( "testCache1", "a:key", "adata" );
    queue.addPutEvent( elem );

    for ( int i = 0; i <= end; i++ )
    {
        synchronized ( this )
        {
            if ( i % 2 == 0 )
            {
                this.wait( idleTime );
            }
            else
            {
                this.wait( idleTime / 2 );
            }
        }
        CacheElement elem2 = new CacheElement( "testCache1", i + ":key", i + "data" );
        queue.addPutEvent( elem2 );
    }
    while ( !queue.isEmpty() )
    {
        synchronized ( this )
        {
            System.out.println( "queue is still busy, waiting 250 millis" );
            this.wait( 250 );
        }
    }
    System.out.println( "queue is empty, comparing putCount" );

    Thread.sleep( 1000 );

    // this becomes less accurate with each test. It should never fail. If
    // it does things are very off.
    assertTrue( "The put count [" + listen.putCount + "] is below the expected minimum threshold ["+ expectedPutCount + "]", listen.putCount >= ( expectedPutCount - 1 ) );

}
}



