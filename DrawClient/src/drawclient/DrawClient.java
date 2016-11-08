/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package drawclient;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;

/**
 *
 * @author Doreen
 */
public class DrawClient extends JPanel 
    implements MouseMotionListener, MouseListener{

    private static class Point
    {
        private int xpos, ypos, time, pgNum;
        private Color color;
        private Point prevPoint;
        public int getXPos() { return xpos;}
        public int getYPos() { return ypos;}
        public int getTime() { return time;}
        public int getPage() { return pgNum;}
        public Point getPrevPoint() {return prevPoint;}
        public void setPrevPoint(Point point){prevPoint = point;}
        public Color getColor() { return color;}
        Point(int x, int y, int t, Color color, int page)
        {
            xpos = x;
            ypos = y;
            time = t;
            this.color = color;
            this.pgNum = page;
        }
    }
    private final ArrayList<Point> points = new ArrayList<>();
    private Point prevPoint;
    private JButton undoButton;
    private JButton prevButton;
    private JButton nextButton;
    private JColorChooser colorChooser;
    private String name;
    private Color color;
    private int page;
    private final JFrame window = new JFrame("Drawing Application");
    
    DrawClient()
    {
        page = 1;
        color = Color.BLACK;
        
        colorChooser = new JColorChooser(color);
        undoButton = new JButton("Undo");
        this.setBackground(Color.WHITE);
        this.addMouseMotionListener(this);
        this.addMouseListener(this);
        prevButton = new JButton("Prev Page");
        nextButton = new JButton("Next Page");
        undoButton.setEnabled(false);
        prevButton.setEnabled(false);
        prevButton.addActionListener(new ActionListener()
                
        {

            @Override
            public void actionPerformed(ActionEvent e) 
            {
                if(page != 1)
                {
                    page--;
                    repaint();
                    if(page == 1)
                        prevButton.setEnabled(false);
                    nextButton.setEnabled(true);
                }
                else
                {
                    prevButton.setEnabled(false);
                }
            }
            
            
        });
        nextButton.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e) 
            {
                prevButton.setEnabled(true);
                page++;
                repaint();
            }
            
            
        });

        undoButton.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e) 
            {
                undo();
            }
               
        });
    }
    
    public void undo()
    {
        synchronized(points)
        {
            
            if(points.size() > 0)
            {
                int index = points.size()-1;
                while(points.get(index).prevPoint != null)
                {
                    points.remove(index);
                    repaint();
                    index--;
                }
                points.remove(index);
                repaint();
                if(points.isEmpty())
                    undoButton.setEnabled(false);
            }
        }
    }
    
    
    @Override
    public void paintComponent(Graphics g){
        super.paintComponent(g);
        synchronized(points)
        {
            for (Point point : points) 
            {
                if(point.pgNum == page)
                {
                    g.setColor(point.getColor());
                    if(point.getPrevPoint() != null)
                        g.drawLine(point.getPrevPoint().getXPos(),
                                   point.getPrevPoint().getYPos(), 
                                   point.getXPos(), 
                                   point.getYPos());
                    //if you press the mouse but dont drag the mouse, still draw a dot
                    else 
                        g.fillRect(point.getXPos(), point.getYPos(), 1,1);
                }
            }
        }
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        
    }

    
    
    
    
    @Override
    public void mouseDragged(MouseEvent e) 
    {
        Point point = new Point(e.getX(),e.getY(),0, color, page);
        point.setPrevPoint(prevPoint);
        points.add(point);
        prevPoint = point;
        repaint();    
        
    }

    @Override
    public void mouseMoved(MouseEvent e) {
    }

    @Override
    public void mouseClicked(MouseEvent e) {
    }

    @Override
    public void mousePressed(MouseEvent e) 
    { 
        undoButton.setEnabled(true);
        //if you press the mouse make a starting point
        Point point = new Point(e.getX(),e.getY(),0, color, page);
        prevPoint = point;
        point.setPrevPoint(null);
        points.add(point);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }
    
}
