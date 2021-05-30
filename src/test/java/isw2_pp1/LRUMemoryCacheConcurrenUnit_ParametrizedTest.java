package isw2_pp1;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import java.util.Arrays;
import java.util.Collection;
import org.apache.jcs.JCS;
import org.apache.jcs.engine.CacheElement;
import org.apache.jcs.engine.behavior.ICacheElement;
import org.apache.jcs.engine.control.CompositeCache;
import org.apache.jcs.engine.control.CompositeCacheManager;
import org.apache.jcs.engine.memory.lru.LRUMemoryCache;
import org.apache.log4j.BasicConfigurator;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.junit.BeforeClass;
import org.junit.Test;

@RunWith(value=Parameterized.class)
public class LRUMemoryCacheConcurrenUnit_ParametrizedTest {
    private static int items = 200;
    private String region;

	public LRUMemoryCacheConcurrenUnit_ParametrizedTest(String region) {
		// TODO Auto-generated constructor stub
		this.region = region;
	}
	
@Parameters
	    public static Collection<Object[]> data() {
	        return Arrays.asList(new Object[][] {
	                { "indexedRegion1"}
	        });
	    }
	
@BeforeClass
public static void Configure()
{
    JCS.setConfigFilename( "/TestDiskCache.ccf" );
}

@Test
/**
 * Adds items to cache, gets them, and removes them. The item count is more
 * than the size of the memory cache, so items should be dumped.
 *
 * @param region
 *            Name of the region to access
 *
 * @exception Exception
 *                If an error occurs
 */
public void runTestForRegion()
        throws Exception
    {
    	CompositeCacheManager cacheMgr = CompositeCacheManager.getUnconfiguredInstance();
        cacheMgr.configure( "/TestDiskCache.ccf" );
        CompositeCache cache = cacheMgr.getCache( region );

        LRUMemoryCache lru = new LRUMemoryCache();
        lru.initialize( cache );

        // Add items to cache

        for ( int i = 0; i < items; i++ )
        {
            ICacheElement ice = new CacheElement( cache.getCacheName(), i + ":key", region + " data " + i );
            ice.setElementAttributes( cache.getElementAttributes() );
            lru.update( ice );
        }

        // Test that initial items have been purged

        for ( int i = 0; i < 102; i++ )
        {
            assertNull( lru.get( i + ":key" ) );
        }

        // Test that last items are in cache

        for ( int i = 102; i < items; i++ )
        {
            String value = (String) lru.get( i + ":key" ).getVal();
            assertEquals( region + " data " + i, value );
        }

        // Remove all the items

        for ( int i = 0; i < items; i++ )
        {
            lru.remove( i + ":key" );
        }

        // Verify removal

        for ( int i = 0; i < items; i++ )
        {
            assertNull( "Removed key should be null: " + i + ":key", lru.get( i + ":key" ) );
        }
    }
}
