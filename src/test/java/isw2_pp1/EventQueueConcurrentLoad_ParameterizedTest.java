package isw2_pp1;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.apache.jcs.engine.CacheElement;
import org.apache.jcs.engine.CacheEventQueue;
import org.apache.log4j.BasicConfigurator;

@RunWith(Parameterized.class)
public class EventQueueConcurrentLoad_ParameterizedTest  {
	private enum Type {PUT_TEST, REMOVE_TEST, STOP_PROCESSING_TEST, PUT_DELAY_TEST}
	private static CacheEventQueue queue = null;
    private static CacheListenerImpl listen = null;
    private static int maxFailure = 3;
    private static int waitBeforeRetry = 100;
    // very small idle time
    private static int idleTime = 2;
	private int end;
	private int expectedPutCount;
	private Type type;

	public EventQueueConcurrentLoad_ParameterizedTest(Type type, int end, int expectedPutCount) {
		super();
		this.type = type; //type of the test
		this.end = end;
		this.expectedPutCount = expectedPutCount;
	}

	
	  @Parameters public static Collection<Object[]> getparamList(){ return
			Arrays.asList(
					new Object[][] { 
										  {Type.PUT_TEST, 200, 200}, 
										  {Type.PUT_TEST, 1200, 1400}, 
										  {Type.REMOVE_TEST,
										  2200, 0}, 
										  {Type.STOP_PROCESSING_TEST, 0, 0}, 
										  {Type.PUT_TEST, 5200, 6600},
										  {Type.REMOVE_TEST, 5200, 0},
										  {Type.STOP_PROCESSING_TEST, 0, 0},
										  {Type.PUT_DELAY_TEST, 100, 6700}, //new parameters 
										  {Type.REMOVE_TEST,6700,0},
										  {Type.STOP_PROCESSING_TEST,0,0}, 
										  {Type.PUT_TEST,100,100},
										  {Type.STOP_PROCESSING_TEST, 0, 0}, 
										  {Type.PUT_DELAY_TEST,120,220},
										  }); }
	 
	@BeforeClass
	 /**
     * Test Configuration. Create the static queue to be used by all tests
     */
	public static void configureCacheQueue() {
    	BasicConfigurator.configure();
		listen = new CacheListenerImpl();
		queue = new CacheEventQueue(listen, 1L, "testCache1", maxFailure, waitBeforeRetry);	
	    queue.setWaitToDieMillis(idleTime);
	}
		
	/**
	 * Adds put events to the queue.
	 * @throws IOException 
	 * @throws InterruptedException 
	 *
	 */
	@Test(timeout=7000)
	public void runPutTest() throws IOException, InterruptedException{
		Assume.assumeTrue(type == Type.PUT_TEST);
		
	    for (int i = 0; i <= end; i++){
	        CacheElement elem = new CacheElement("testCache1", i + ":key", i + "data");
	        queue.addPutEvent(elem);
	    }
	
	    while (!queue.isEmpty()){
	        synchronized (this){
	            System.out.println("queue is still busy, waiting 250 millis");
	            this.wait(250);
	        }
	    }
	    System.out.println("queue is empty, comparing putCount");
	
	    //This becomes less accurate with each test. It should never fail. If it does things are very off.
	    assertTrue("The put count [" + listen.putCount + "] is below the expected minimum threshold [" + expectedPutCount + "]", listen.putCount >= (expectedPutCount - 1));
	}
	
	/**
	 * Add remove events to the event queue.
	 *
	 * @throws IOException
	 */
	@Test(timeout=7000)
	public void runRemoveTest() throws IOException {
		Assume.assumeTrue(type == Type.REMOVE_TEST);
		
	    for ( int i = 0; i <= end; i++ ){
	        queue.addRemoveEvent( i + ":key" );
	    }
	}
	
	/**
	 * Add remove events to the event queue.
	 *
	 */
	@Test(timeout=7000)
	public void runStopProcessingTest(){
		Assume.assumeTrue(type == Type.STOP_PROCESSING_TEST);
	    queue.stopProcessing();
	}

	/**
	 * Test putting and a delay. Waits until queue is empty to start.
	 *
	 * @param end
	 * @param expectedPutCount
	 * @throws InterruptedException 
	 * @throws IOException
	 */
	@Test(timeout=7000)
	public void runPutDelayTest() throws InterruptedException, IOException{
		Assume.assumeTrue(type == Type.PUT_DELAY_TEST);
		
	    while (!queue.isEmpty()){
	        synchronized ( this ){
	            System.out.println( "queue is busy, waiting 250 millis to begin" );
	            this.wait( 250 );
	        }
	    }
	    System.out.println( "queue is empty, begin" );
	
	    // get it going
	    CacheElement elem = new CacheElement("testCache1", "a:key", "adata" );
	    queue.addPutEvent( elem );
	
	    for (int i = 0; i <= end; i++){
	        synchronized (this){
	            if (i % 2 == 0){
	                this.wait(idleTime);
	            } else {
	                this.wait(idleTime / 2);
	            }
	        }
	        CacheElement elem2 = new CacheElement("testCache1", i + ":key", i + "data" );
	        queue.addPutEvent( elem2 );
	    }
	
	    while (!queue.isEmpty()){
	        synchronized (this){
	            System.out.println( "queue is still busy, waiting 250 millis" );
	            this.wait( 250 );
	        }
	    }
	    System.out.println( "queue is empty, comparing putCount" );
	
	    Thread.sleep(1000);
	
	    // this becomes less accurate with each test. It should never fail. If
	    // it does things are very off.
	    assertTrue("The put count [" + listen.putCount + "] is below the expected minimum threshold [" + expectedPutCount + "]", listen.putCount >= ( expectedPutCount - 1 ) );
	
	}
}