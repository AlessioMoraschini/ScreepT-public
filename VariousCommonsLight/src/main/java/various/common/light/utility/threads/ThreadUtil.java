package various.common.light.utility.threads;

import java.util.concurrent.atomic.AtomicInteger;

public class ThreadUtil {

	public static final AtomicInteger AVAILABLE_GLOBAL_THREADS = new AtomicInteger(1);
	public static final AtomicInteger MAX_THREADS_FOLDER = new AtomicInteger(1);


}
