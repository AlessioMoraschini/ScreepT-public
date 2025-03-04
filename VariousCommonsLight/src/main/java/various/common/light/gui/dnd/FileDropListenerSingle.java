package various.common.light.gui.dnd;


/**
 * Implement this inner interface to listen for when files are dropped. For example
 * your class declaration may begin like this:
 * <code><pre>
 *      public class MyClass implements FileDrop.Listener
 *      ...
 *      public void filesDropped( java.io.File[] files )
 *      {
 *          ...
 *      }   // end filesDropped
 *      ...
 * </pre></code>
 *
 * @since 1.8
 */
@FunctionalInterface
public interface FileDropListenerSingle {

        /**
         * This method is called when files have been successfully dropped.
         *
         * @param files An array of <tt>File</tt>s that were dropped.
         * @since 1.0
         */
        public abstract void fileDropped( java.io.File file );
        
}
