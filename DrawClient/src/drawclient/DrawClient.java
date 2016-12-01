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
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.imageio.ImageIO;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.colorchooser.AbstractColorChooserPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileNameExtensionFilter;


public class DrawClient extends JPanel 
    implements MouseMotionListener, MouseListener, ChangeListener{

    public static class Point implements Serializable
    {
        private int xpos, ypos, pgNum;
        private long time;
        private Color color;
        private int stroke;
        
        public int getXPos() { return xpos;}
        public int getYPos() { return ypos;}
        public long getTime() { return time;}
        public int getPage() { return pgNum;}
        public Color getColor() { return color;}
        public int getStroke() {return stroke;}
        Point(int x, int y, long t, Color color, int page, int stroke)
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
    private final JComboBox comboBox;
    private Color color;
    private int page;
    private final JScrollPane scrollPanel;
    private final JPanel mainPanel = new JPanel(new GridBagLayout());
    private int strokeSize = 1;
    private Color lastColor = Color.BLACK;
    private boolean recording = false;
    private ObjectOutputStream oos;
    private long startTime;
    private TargetDataLine tdl;
    private AudioFormat af;
    
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
        String[] strokeSizes = { "1", "2", "3", "4", "5" };
        comboBox = new JComboBox(strokeSizes);
        comboBox.setEnabled(true);
        comboBox.setEditable(false);
        //allows you to change stroke size
        comboBox.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e) 
            {
                strokeSize = comboBox.getSelectedIndex()+1;
            }
               
        });
        
        
        //allows you to change pages(previous page)
        prevButton.addActionListener(new ActionListener()
        {

            @Override
            public void actionPerformed(ActionEvent e) 
            {
                if(recording)
                {
                    try {
                        long t = System.nanoTime()-startTime;
                        oos.writeUTF("/P");
                        oos.writeLong(t);
                        oos.flush();
                    } catch (IOException ex) {
                        Logger.getLogger(DrawClient.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
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
                
                if(recording)
                {
                    try {
                        long t = System.nanoTime()-startTime;
                        oos.writeUTF("/N");
                        oos.writeLong(t);
                        oos.flush();
                    } catch (IOException ex) {
                        Logger.getLogger(DrawClient.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
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
                color = Color.WHITE;
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
                eraserButton.setEnabled(true);
                drawButton.setEnabled(false);
            }
               
        });
        
        recordButton.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e) 
            {
                if(!recording)
                {
                    int val =JOptionPane.showConfirmDialog(null, "Are you sure you want to record? "
                            + "(This will erase all previous drawings.)",
                            "Recording Confirmation",
                            JOptionPane.YES_NO_OPTION);
                    if(val == JOptionPane.YES_OPTION)
                    {
                        try {
                            oos = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream("temp.txt")));
                            
                            //setup audio capture
                            af = new AudioFormat(16000,16,2,true,true);
                            DataLine.Info info = new DataLine.Info(
                                TargetDataLine.class,af);
                            tdl = (TargetDataLine) AudioSystem.getLine(info);
                            tdl.open(af);
                            
                            recording = true;
                            
                            Thread recAudio = new Thread(new RecordAudio());
                            recAudio.start();
                            
                            startTime = System.nanoTime();
                            Image img;
                            img = ImageIO.read(getClass().getResource("/resources/StopButton.png"));
                            img = img.getScaledInstance( 50, 50,  java.awt.Image.SCALE_SMOOTH ) ;
                            recordButton.setIcon(new ImageIcon(img));
                            page = 0;
                            directory.clear();
                            prevButton.setEnabled(false);
                            nextButton.setEnabled(true);
                            undoButton.setEnabled(false);
                            //init first page
                            directory.add(new ArrayList<>());
                            repaint();
                        } catch (IOException ex) {
                            JOptionPane.showMessageDialog(null,"Error Occurred creating file!");
                        } catch (LineUnavailableException ex) {
                            Logger.getLogger(DrawClient.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
                else
                {
                    try {
                        Image img;
                        img = ImageIO.read(getClass().getResource("/resources/RecordButton.png"));
                        img = img.getScaledInstance( 50, 50,  java.awt.Image.SCALE_SMOOTH ) ;
                        recordButton.setIcon(new ImageIcon(img));
                        recording = false;
                        oos.writeUTF("/e");
                        oos.flush();
                        oos.close();
                    } catch (IOException ex) {
                        System.exit(1);
                    }
                }
            }
               
        });
        
        
        
        
        Image img;
        try {
            img = ImageIO.read(getClass().getResource("/resources/UndoButton.png"));
            img = img.getScaledInstance( 50, 50,  java.awt.Image.SCALE_SMOOTH );
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
            System.exit(1);
        }
        
       
        JLabel label = new JLabel("Stroke"); 
        JPanel strokePane = new JPanel(new GridBagLayout());
        scrollPanel = new JScrollPane(this);
        JPanel panel = new JPanel(new GridBagLayout());
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
        
        c.insets = new Insets(5,10,0,0);
        c.gridx = 4;
        c.gridy = 0;
        c.gridwidth = 3;
        c.gridheight = 1;
        panel.add(colorChooser,c);
        
        c.insets = new Insets(10,10,0,0);
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
        
        
        c.gridx = 0;
        c.gridy = 0;
        
        strokePane.add(label,c);
        
        c.gridx = 0;
        c.gridy = 1;
        
        strokePane.add(comboBox,c);
        
        
        
        c.gridx = 9;
        c.gridy = 0;
        c.gridwidth = 1;
        c.gridheight = 1;
        
        panel.add(strokePane,c);
        
        c.insets = new Insets(0,0,0,0);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;
        mainPanel.add(panel,c);
        
        
        c.fill = GridBagConstraints.BOTH;
        c.gridx = 0;
        c.gridy = 1;
        c.weighty = 1;
        c.weightx = 1;
        mainPanel.add(scrollPanel,c);
        
    }
    
    private class RecordAudio implements Runnable
    {

        @Override
        public void run() 
        {
            try {
                tdl.start();
                
                byte[] buffer = new byte[4096];
                int bytesRead = 0;
                
                ByteArrayOutputStream recordBytes = new ByteArrayOutputStream();
                
                while (recording) {
                    bytesRead = tdl.read(buffer, 0, buffer.length);
                    recordBytes.write(buffer, 0, bytesRead);
                }
                if (tdl != null)
                {
                    tdl.flush();
                    tdl.close();
                }
                byte[] audioData = recordBytes.toByteArray();
                ByteArrayInputStream bais = new ByteArrayInputStream(audioData);
                AudioInputStream audioInputStream = new AudioInputStream(bais, af,
                        audioData.length / af.getFrameSize());
                File tempFile = new File("temp.wav");
                AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, tempFile);
                
                audioInputStream.close();
                recordBytes.close();
                
                
                JFileChooser chooser = new JFileChooser(System.getProperty("user.dir"));
                chooser.setFileFilter(new FileNameExtensionFilter(null,"zip"));
                File newFil;
                int retrieval = chooser.showSaveDialog(null);
                if (retrieval == JFileChooser.APPROVE_OPTION) 
                {
                    if(!chooser.getSelectedFile().getAbsolutePath().endsWith(".zip"))
                        newFil = new File(chooser.getSelectedFile() + ".zip");
                    else
                        newFil = chooser.getSelectedFile();
                    
                }
                else
                {
                    newFil = new File("untitled.zip");

                }
                FileOutputStream fos = new FileOutputStream(newFil);

                ZipOutputStream out =  new ZipOutputStream(fos);
                
                String [] filNames = {"temp.txt","temp.wav"};
                
                for(int i = 0; i < filNames.length; i++)
                {
                    File srcFile = new File(filNames[i]);
                    FileInputStream fis = new FileInputStream(srcFile);
                    out.putNextEntry(new ZipEntry(srcFile.getName()));
                    
                    int length;
                    
                    while ((length = fis.read(buffer)) > 0) 
                        out.write(buffer, 0, length);
                        
                    out.closeEntry();
                    
                    fis.close();
                    Files.delete(srcFile.toPath());
                }
                out.close();
                
                
                
            } catch (IOException ex) {
                Logger.getLogger(DrawClient.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            
        }

        
    }
    
    
    
    
    @Override
    public Dimension getPreferredSize()
    {
        return Toolkit.getDefaultToolkit().getScreenSize();
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
        drawButton.setEnabled(false);
        eraserButton.setEnabled(true);
    }
    
    //undo last shape
    public void undo()
    {
        synchronized(directory)
        {
            if(recording)
            {
                long t = System.nanoTime()-startTime;
                try {
                    oos.writeUTF("/U");
                    oos.writeLong(t);
                    oos.flush();
                } catch (IOException ex) {
                    Logger.getLogger(DrawClient.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
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
        g2.setRenderingHint(
        RenderingHints.KEY_ANTIALIASING,
        RenderingHints.VALUE_ANTIALIAS_ON);
        super.paintComponent(g2);
        synchronized(directory)
        {
            int l = 0;
            for (ArrayList<Point> ary : directory.get(page)) 
            {
                for(Point point : ary)
                {
                    g2.setPaint(point.getColor());
                    g2.setStroke(new BasicStroke(point.getStroke()));
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
                        g2.drawLine(point.getXPos(),
                                   point.getYPos(), 
                                   point.getXPos(), 
                                   point.getYPos());
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
        
        
        window.setContentPane(panel.getPanel());
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
        long t = System.nanoTime()-startTime;
        Point point = new Point(e.getX(),e.getY(),t, color, page,strokeSize);
        directory.get(page).get(directory.get(page).size()-1).add(point);
        if(recording)
        {
            try 
            {
                oos.writeUTF("/d");
                oos.writeObject(point);
                oos.flush();
            } catch (IOException ex) {
                Logger.getLogger(DrawClient.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
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
        long t = System.nanoTime()-startTime;
        Point point = new Point(e.getX(),e.getY(),t, color, page,strokeSize);
        directory.get(page).get(directory.get(page).size()-1).add(point);
        if(recording)
        {
            try 
            {
                oos.writeUTF("/p");
                oos.writeObject(point);
                oos.flush();
            } catch (IOException ex) {
                Logger.getLogger(DrawClient.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        repaint();
        
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
