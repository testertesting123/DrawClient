/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package drawclient;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
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
import javax.swing.JScrollPane;
import javax.swing.colorchooser.AbstractColorChooserPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 *
 * @author Doreen
 */
public class DrawClient extends JPanel 
    implements MouseMotionListener, MouseListener, ChangeListener{

    private static class Point
    {
        private int xpos, ypos, time, pgNum;
        private Color color;
        public int getXPos() { return xpos;}
        public int getYPos() { return ypos;}
        public int getTime() { return time;}
        public int getPage() { return pgNum;}
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
    private final ArrayList<ArrayList<ArrayList<Point>>> directory = new ArrayList<>();
    private final JButton undoButton;
    private JButton prevButton;
    private JButton nextButton;
    private final JColorChooser colorChooser;
    private Color color;
    private int page;
    private final JScrollPane scrollPanel;
    private final JPanel panel;
    DrawClient()
    {
        page = 0;
        color = Color.BLACK;
        
        //init first page
        directory.add(new ArrayList<>());
        
        colorChooser = new JColorChooser(color);
        //remove preview panel from color chooser
        colorChooser.setPreviewPanel(new JPanel());
        
        //keep only the swatches panel for jcolorchooser
        AbstractColorChooserPanel[] panels = colorChooser.getChooserPanels();
        for (AbstractColorChooserPanel accp : panels) {
            if (!accp.getDisplayName().equals("Swatches")) {
                colorChooser.removeChooserPanel(accp);
            }
        }
        //add changelistener which detects when a new color is chose
        colorChooser.getSelectionModel().addChangeListener(this);
        
        undoButton = new JButton("Undo");
        this.setBackground(Color.WHITE);
        this.addMouseMotionListener(this);
        this.addMouseListener(this);
        prevButton = new JButton("Prev Page");
        nextButton = new JButton("Next Page");
        undoButton.setEnabled(false);
        prevButton.setEnabled(false);
        
        //allows you to change pages(previous page)
        prevButton.addActionListener(new ActionListener()
        {

            @Override
            public void actionPerformed(ActionEvent e) 
            {
                if(page != 0)
                {
                    page--;
                    if(page == 0)
                        prevButton.setEnabled(false);
                    nextButton.setEnabled(true);
                    if(!directory.get(page).isEmpty())
                        undoButton.setEnabled(true);
                    else
                        undoButton.setEnabled(false);
                    repaint();
                }
                else
                {
                    prevButton.setEnabled(false);
                }
            }
            
            
        });
        //allows you to change pages(next page)
        nextButton.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e) 
            {
                
                prevButton.setEnabled(true);
                page++;
                try
                {
                    directory.get(page);
                }
                catch(IndexOutOfBoundsException ie)
                {
                    directory.add(new ArrayList<>());
                }
                if(!directory.get(page).isEmpty())
                    undoButton.setEnabled(true);
                else
                    undoButton.setEnabled(false);
                repaint();
            }
            
            
        });

        //undos the last point to point segment or drawing
        undoButton.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e) 
            {
                undo();
            }
               
        });
        
        setPreferredSize(Toolkit.getDefaultToolkit().getScreenSize());
        scrollPanel = new JScrollPane(this);
        panel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        
        c.anchor = GridBagConstraints.FIRST_LINE_START;
        c.gridx = 0;
        c.gridy = 0;
        c.insets = new Insets(10,10,0,0);
        panel.add(undoButton,c);
        
        c.anchor = GridBagConstraints.PAGE_START;
        c.gridx = 1;
        c.gridy = 0;
        panel.add(prevButton,c);
        
        c.gridx = 2;
        c.gridy = 0;
        panel.add(nextButton,c);
        
//      c.gridx = 3;
//      c.gridy = 0;
//      panel.add(recordButton,c);
        
        c.gridx = 4;
        c.gridy = 0;
        c.gridwidth = 3;
        c.gridheight = 1;
        panel.add(colorChooser,c);
        
//        c.gridx = 0;
//        c.gridy = 0;
//        c.gridwidth = 1;
//        c.gridheight = 1;
//        panel.add(eraserButton,c);
        
        c.fill = GridBagConstraints.BOTH;
        c.gridx = 0;
        c.gridy = 1;
        c.weighty = 1;
        c.weightx = 1;
        panel.add(scrollPanel,c);
    }
    
    public JPanel getPanel()
    {
        return panel;
    }
    
    //for detecting color changes
    @Override
    public void stateChanged(ChangeEvent e) 
    {
        color = colorChooser.getColor();
        System.out.println(color);
    }
    
    //undo last shape
    public void undo()
    {
        synchronized(directory)
        {
            
            if(directory.get(page).size() > 0)
            {
                directory.get(page).remove(directory.get(page).size()-1);
                repaint();
                if(directory.get(page).isEmpty())
                    undoButton.setEnabled(false);
            }
        }
    }
    
    //paint each point as it is received
    @Override
    public void paintComponent(Graphics g)
    {
        super.paintComponent(g);
        synchronized(directory)
        {
            int l = 0;
            for (ArrayList<Point> ary : directory.get(page)) 
            {
                for(Point point : ary)
                {
                    g.setColor(point.getColor());
                    int index = directory.get(page).get(l).indexOf(point);
                    if(index != 0)
                        g.drawLine(directory.get(page).get(l).get(index-1).getXPos(),
                                   directory.get(page).get(l).get(index-1).getYPos(), 
                                   point.getXPos(), 
                                   point.getYPos());
                    //if you press the mouse but dont drag the mouse, still draw a dot
                    else 
                        g.fillRect(point.getXPos(), point.getYPos(), 1,1);
                    
                }
                l++;
            }
        }
    }
    
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        JFrame window = new JFrame("Drawing Application");
        DrawClient panel = new DrawClient();
        
        
        window.add(panel.getPanel());
        Dimension dimen = Toolkit.getDefaultToolkit().getScreenSize();
        window.setPreferredSize(new Dimension(dimen.width/2,dimen.height/2));
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setLocation(100,100);
        window.setExtendedState(JFrame.MAXIMIZED_BOTH); 
        
        window.pack();
        window.setLocationRelativeTo(null);
        window.setVisible(true);
    }

    
    
    
    
    @Override
    public void mouseDragged(MouseEvent e) 
    {
        Point point = new Point(e.getX(),e.getY(),0, color, page);
        directory.get(page).get(directory.get(page).size()-1).add(point);
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
        directory.get(page).add(new ArrayList<>());
        //if you press the mouse make a starting point
        Point point = new Point(e.getX(),e.getY(),0, color, page);
        directory.get(page).get(directory.get(page).size()-1).add(point);
        
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
