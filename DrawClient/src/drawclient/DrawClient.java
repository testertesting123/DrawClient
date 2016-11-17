/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package drawclient;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
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
        private BasicStroke stroke;
        
        public int getXPos() { return xpos;}
        public int getYPos() { return ypos;}
        public int getTime() { return time;}
        public int getPage() { return pgNum;}
        public Color getColor() { return color;}
        public BasicStroke getStroke() {return stroke;}
        Point(int x, int y, int t, Color color, int page, BasicStroke stroke)
        {
            xpos = x;
            ypos = y;
            time = t;
            this.color = color;
            this.pgNum = page;
            this.stroke = stroke;
        }
    }
    private final ArrayList<ArrayList<ArrayList<Point>>> directory = new ArrayList<>();
    private final JButton undoButton;
    private final JButton prevButton;
    private final JButton nextButton;
    private final JButton recordButton;
    private final JButton eraserButton;
    private final JButton drawButton;
    private final JColorChooser colorChooser;
    private Color color;
    private int page;
    private final JScrollPane scrollPanel;
    private final JPanel mainPanel = new JPanel();
    private BasicStroke strokeSize = new BasicStroke(1);
    private BasicStroke lastStroke = strokeSize;
    private Color lastColor = Color.BLACK;
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
        recordButton = new JButton("Record");
        eraserButton = new JButton("Eraser");
        drawButton = new JButton("Draw");
        
        undoButton.setEnabled(false);
        prevButton.setEnabled(false);
        recordButton.setEnabled(true);
        eraserButton.setEnabled(true);
        drawButton.setEnabled(false);
        
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
        
        eraserButton.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e) 
            {
                lastColor = color;
                lastStroke = strokeSize;
                color = Color.WHITE;
                strokeSize = new BasicStroke(10);
                eraserButton.setEnabled(false);
                drawButton.setEnabled(true);
            }
               
        });
                
        drawButton.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e) 
            {
                color = lastColor;
                strokeSize = lastStroke;
                eraserButton.setEnabled(true);
                drawButton.setEnabled(false);
            }
               
        });
        
        Image img;
        try {
            img = ImageIO.read(getClass().getResource("/resources/UndoButton.png"));
            img = img.getScaledInstance( 50, 50,  java.awt.Image.SCALE_SMOOTH ) ;
            undoButton.setIcon(new ImageIcon(img));
            undoButton.setRolloverEnabled(true);
            undoButton.setMargin(new Insets(0, 0, 0, 0));
            undoButton.setContentAreaFilled(false);
            undoButton.setVerticalTextPosition(SwingConstants.BOTTOM);
            undoButton.setHorizontalTextPosition(SwingConstants.CENTER);
            
            img = ImageIO.read(getClass().getResource("/resources/BackButton.png"));
            img = img.getScaledInstance( 50, 50,  java.awt.Image.SCALE_SMOOTH ) ;
            prevButton.setIcon(new ImageIcon(img));
            prevButton.setMargin(new Insets(0, 0, 0, 0));
            prevButton.setContentAreaFilled(false);
            prevButton.setVerticalTextPosition(SwingConstants.BOTTOM);
            prevButton.setHorizontalTextPosition(SwingConstants.CENTER);
            
            img = ImageIO.read(getClass().getResource("/resources/ForwardButton.png"));
            img = img.getScaledInstance( 50, 50,  java.awt.Image.SCALE_SMOOTH ) ;
            nextButton.setIcon(new ImageIcon(img));
            nextButton.setMargin(new Insets(0, 0, 0, 0));
            nextButton.setContentAreaFilled(false);
            nextButton.setVerticalTextPosition(SwingConstants.BOTTOM);
            nextButton.setHorizontalTextPosition(SwingConstants.CENTER);
            
            img = ImageIO.read(getClass().getResource("/resources/RecordButton.png"));
            img = img.getScaledInstance( 50, 50,  java.awt.Image.SCALE_SMOOTH ) ;
            recordButton.setIcon(new ImageIcon(img));
            recordButton.setMargin(new Insets(0, 0, 0, 0));
            recordButton.setContentAreaFilled(false);
            recordButton.setVerticalTextPosition(SwingConstants.BOTTOM);
            recordButton.setHorizontalTextPosition(SwingConstants.CENTER);
            
            img = ImageIO.read(getClass().getResource("/resources/Eraser.png"));
            img = img.getScaledInstance( 50, 50,  java.awt.Image.SCALE_SMOOTH ) ;
            eraserButton.setIcon(new ImageIcon(img));
            eraserButton.setMargin(new Insets(0, 0, 0, 0));
            eraserButton.setContentAreaFilled(false);
            eraserButton.setVerticalTextPosition(SwingConstants.BOTTOM);
            eraserButton.setHorizontalTextPosition(SwingConstants.CENTER);
            
            img = ImageIO.read(getClass().getResource("/resources/pencil.png"));
            img = img.getScaledInstance( 50, 50,  java.awt.Image.SCALE_SMOOTH ) ;
            drawButton.setIcon(new ImageIcon(img));
            drawButton.setMargin(new Insets(0, 0, 0, 0));
            drawButton.setContentAreaFilled(false);
            drawButton.setVerticalTextPosition(SwingConstants.BOTTOM);
            drawButton.setHorizontalTextPosition(SwingConstants.CENTER);
            
        } catch (IOException ex) {
            Logger.getLogger(DrawClient.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        
        
        
        JPanel panel = new JPanel();
        
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
        
        c.gridx = 3;
        c.gridy = 0;
        panel.add(recordButton,c);
        
        c.gridx = 4;
        c.gridy = 0;
        c.gridwidth = 3;
        c.gridheight = 1;
        panel.add(colorChooser,c);
        
        c.gridx = 7;
        c.gridy = 0;
        c.gridwidth = 1;
        c.gridheight = 1;
        panel.add(eraserButton,c);
        
        c.gridx = 8;
        c.gridy = 0;
        c.gridwidth = 1;
        c.gridheight = 1;
        panel.add(drawButton,c);
        
        c.fill = GridBagConstraints.BOTH;
        c.gridx = 0;
        c.gridy = 1;
        c.weighty = .2;
        mainPanel.add(panel,c);
        
        
        c.fill = GridBagConstraints.BOTH;
        c.gridx = 0;
        c.gridy = 1;
        c.weighty = .8;
        mainPanel.add(scrollPanel,c);
        
    }
    
    public JPanel getPanel()
    {
        return mainPanel;
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
        Graphics2D g2 = (Graphics2D) g;
        super.paintComponent(g2);
        synchronized(directory)
        {
            int l = 0;
            for (ArrayList<Point> ary : directory.get(page)) 
            {
                for(Point point : ary)
                {
                    g2.setPaint(point.getColor());
                    g2.setStroke(point.getStroke());
                    int index = directory.get(page).get(l).indexOf(point);
                    if(index != 0)
                    {
                        g2.drawLine(directory.get(page).get(l).get(index-1).getXPos(),
                                   directory.get(page).get(l).get(index-1).getYPos(), 
                                   point.getXPos(), 
                                   point.getYPos());
                    }
                    //if you press the mouse but dont drag the mouse, still draw a dot
                    else 
                    {
                        g2.fillRect(point.getXPos(), point.getYPos(), 1,1);
                    }
                    
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
        Point point = new Point(e.getX(),e.getY(),0, color, page,strokeSize);
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
        Point point = new Point(e.getX(),e.getY(),0, color, page,strokeSize);
        directory.get(page).get(directory.get(page).size()-1).add(point);
        
    }

    @Override
    public void mouseReleased(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
    }

    @Override
    public void mouseExited(MouseEvent e) {
        setCursor(Cursor.getDefaultCursor());
    }
    
}
