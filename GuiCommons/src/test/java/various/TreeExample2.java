package various;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultTreeCellRenderer;

public class TreeExample2 {

     public static Color selectedColor = new Color(115,164,209);
     public static Color selectedBorderColor = new Color(57,105,138);
     
     public static void main(String args[]) {
          
          JFrame f = new JFrame("JTree Sample");
          f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
          Container content = f.getContentPane();

          
          // custom tree handling
          final JTree tree = new MyTree();
          MyTreeCellRenderer renderer = new MyTreeCellRenderer();
          renderer.setBorderSelectionColor(null); // remove selection border
          renderer.setBackgroundSelectionColor( null); // remove selection background since we paint the selected row ourselves
          tree.setCellRenderer( renderer);

       // send repaint event when node selection changes; otherwise sometimes the border remained visible although the selected node changed 
          TreeSelectionListener treeSelListener = new TreeSelectionListener() {
               public void valueChanged(TreeSelectionEvent evt) {
                    tree.treeDidChange();
               }
          };
          tree.addTreeSelectionListener(treeSelListener);
          
          // handle mouse clicks outside of the node's label
          // mouse begin ----------------------------------------------------------------
          MouseListener ml = new MouseAdapter() {
               public void mousePressed(MouseEvent e) {
                    int selRow = tree.getClosestRowForLocation( e.getX(), e.getY());
                    if( selRow != -1) {
                         Rectangle bounds = tree.getRowBounds( selRow);
                         boolean outside = e.getX() < bounds.x || e.getX() > bounds.x + bounds.width || e.getY() < bounds.y || e.getY() >= bounds.y + bounds.height;
                         if( outside) {
                              
                              tree.setSelectionRow(selRow);
                              System.out.println( "manual selection: " + selRow);
                              
                              // handle doubleclick
                              if( e.getClickCount() == 2) {
                                   if( tree.isCollapsed(selRow))
                                        tree.expandRow( selRow);
                                   else if( tree.isExpanded( selRow))
                                        tree.collapseRow( selRow);
                              }
                              
                         } else {
                              System.out.println( "auto selection: " + selRow);
                         }
                    }
               }
          };
          tree.addMouseListener(ml);          
          // mouse end ----------------------------------------------------------------
          
          // add tree to frame and show frame
          JScrollPane scrollPane = new JScrollPane(tree);
          content.add(scrollPane, BorderLayout.CENTER);
          f.setSize(400, 600);
          f.setVisible(true);
     }

     public static class MyTree extends JTree {
		private static final long serialVersionUID = 3635129823226127167L;

		protected void paintComponent(Graphics g) {

               // paint background
               g.setColor(getBackground());
               g.fillRect(0, 0, getWidth(), getHeight());

               // paint selected node's background and border
               int fromRow = getRowForPath( getSelectionPath());
               if (fromRow != -1) {
                    int toRow = fromRow + 1;
                    Rectangle fromBounds = getRowBounds(fromRow);
                    Rectangle toBounds = getRowBounds(toRow - 1);
                    if (fromBounds != null && toBounds != null) {
                         g.setColor(selectedColor);
                         g.fillRect(0, fromBounds.y, getWidth(), toBounds.y - fromBounds.y + toBounds.height);
                         g.setColor(selectedBorderColor);
                         g.drawRect(0, fromBounds.y, getWidth() - 1, toBounds.y - fromBounds.y + toBounds.height);
                    }
               }

               // perform operation of superclass
               setOpaque(false); // trick not to paint background
               super.paintComponent(g);
               setOpaque(false);
          }
     }

     // trick to make renderer transparent for the unselected nodes 
     public static class MyTreeCellRenderer extends DefaultTreeCellRenderer {
		private static final long serialVersionUID = 11216499812321L;

		public MyTreeCellRenderer() {
               setBackgroundNonSelectionColor(null);
          }

          public Color getBackground() {
               return null;
          }
     }
}
