/*
 * ImageComponents.java
 * A4 Solution by Ivy Wong || wongivy
 * 
 * 
 * CSE 373, University of Washington, Autumn 2014.
 * 
 * Starter Code for CSE 373 Assignment 4, Part II.    Starter Code Version 0.3.
 * S. Tanimoto,  Nov. 6, 2014, after feedback from TA Johnson Goh.
 * Minor changes from v0.2: removed blockSize and hashFunctionChoice variables needed in A3,
 *   added a new variable parentID to standardize the way solutions look for A4,
 *   and added stubs for the find and union methods that need to be implemented.
 * 
 */ 

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ByteLookupTable;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.awt.image.LookupOp;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.util.PriorityQueue;

public class ImageComponents extends JFrame implements ActionListener {
    public static ImageComponents appInstance; // Used in main().

    String startingImage = "donut2.png";
    BufferedImage biTemp, biWorking, biFiltered; // These hold arrays of pixels.
    Graphics gOrig, gWorking; // Used to access the drawImage method.
    int w; // width of the current image.
    int h; // height of the current image.

    int[][] parentID; // For your forest of up-trees.

    private static final int NREGIONS_CHECK = 0;
    private static final int DELTA_CHECK = 1;

    /*
     * Finds and returns the pixelID of the parent of the given PixelID
     *
     * @param pixelID ID of a pixel in the image
     * @return the parent of the given pixel
     */
    int find(int pixelID) {
        while(parentID[getYcoord(pixelID)][getXcoord(pixelID)] != -1) {
            pixelID = parentID[getYcoord(pixelID)][getXcoord(pixelID)];
        }
        return pixelID;
    }// Part of your UNION-FIND implementation. You need to complete the implementation of this.

    /*
     * Unions the two given pixelIDs with the smaller pixelID of root becoming the parent of the union
     *
     * @param pixelID1 One pixelID
     * @param pixelID2 Second pixelID
     */
    void union(int pixelID1, int pixelID2) {
        pixelID1 = find(pixelID1);      // finds the root of the first pixelID
        pixelID2 = find(pixelID2);      // finds the root of the second pixelID
        int x1 = getXcoord(pixelID1);
        int x2 = getXcoord(pixelID2);
        int y1 = getYcoord(pixelID1);
        int y2 = getYcoord(pixelID2);
        if(pixelID1 < pixelID2) {
            parentID[y2][x2] = pixelID1;
        } else {
            parentID[y1][x1] = pixelID2;
        }
    }  // Another part of your UNION-FIND implementation.  Also complete this one.

    /*
     * Returns the x coordinate of the given pixel
     *
     * @param pixelID ID of a pixel in the image
     * @return X coordinate of the pixel in the image
     */
    private int getXcoord(int pixelID) {
        return pixelID % w;
    }

    /*
     * Returns the y coordinate of the given pixel
     *
     * @param pixelID ID of the pixel in the image
     * @return Y coordinate of the pixel in teh image
     */
    private int getYcoord(int pixelID) {
        return pixelID / w;
    }

    /*
     * Initializes the uptree before any unions have been made
     */
    private void initializeParentID() {
        parentID = new int[h][w];
        for(int y = 0; y < h ; y++) {
            for (int x = 0; x < w; x++) {
                parentID[y][x] = -1;
            }
        }
    }

    JPanel viewPanel; // Where the image will be painted.
    JPopupMenu popup;
    JMenuBar menuBar;
    JMenu fileMenu, imageOpMenu, ccMenu, helpMenu;
    JMenuItem loadImageItem, saveAsItem, exitItem;
    JMenuItem lowPassItem, highPassItem, photoNegItem, RGBThreshItem;

    JMenuItem CCItem1;
    JMenuItem aboutItem, helpItem;
    JMenuItem CCItem2;
    JMenuItem CCItem3;
    
    JFileChooser fileChooser; // For loading and saving images.
    
    public class Color {
        int r, g, b;

        Color(int r, int g, int b) {
            this.r = r; this.g = g; this.b = b;    		
        }

        int euclideanDistance(Color c2) {
            // TODO
            // Replace this to return the distance between this color and c2.
            int diffR = r - c2.r; // calculates the distance between the red
            int diffG = g - c2.g; // calculates the distance between the blue
            int diffB = b - c2.b; // calculates the distance between the green
            return diffR * diffR + diffG * diffG + diffB * diffB;
        }
    }

    public class Edge implements Comparable<Edge>{
        private int endpoint0, endpoint1, weight;

        Edge(int endpoint0, int endpoint1, int weight) {
            this.endpoint0 = endpoint0;
            this.endpoint1 = endpoint1;
            this.weight = weight;
        }

        public int compareTo(Edge e2) {
            if(weight == e2.getWeight()) {
                return 0;
            } else if(weight < e2.getWeight()) {
                return -1;
            } else {
                return 1;
            }
        }

        public int getEndpoint0() {
            return endpoint0;
        }

        public int getEndpoint1() {
            return endpoint1;
        }

        public  int getWeight() {
            return weight;
        }
    }

    // Some image manipulation data definitions that won't change...
    static LookupOp PHOTONEG_OP, RGBTHRESH_OP;
    static ConvolveOp LOWPASS_OP, HIGHPASS_OP;
    
    public static final float[] SHARPENING_KERNEL = { // sharpening filter kernel
        0.f, -1.f,  0.f,
       -1.f,  5.f, -1.f,
        0.f, -1.f,  0.f
    };

    public static final float[] BLURRING_KERNEL = {
        0.1f, 0.1f, 0.1f,    // low-pass filter kernel
        0.1f, 0.2f, 0.1f,
        0.1f, 0.1f, 0.1f
    };
    
    public ImageComponents() { // Constructor for the application.
        setTitle("Image Analyzer"); 
        addWindowListener(new WindowAdapter() { // Handle any window close-box clicks.
            public void windowClosing(WindowEvent e) {System.exit(0);}
        });

        // Create the panel for showing the current image, and override its
        // default paint method to call our paintPanel method to draw the image.
        viewPanel = new JPanel(){public void paint(Graphics g) { paintPanel(g);}};
        add("Center", viewPanel); // Put it smack dab in the middle of the JFrame.

        // Create standard menu bar
        menuBar = new JMenuBar();
        setJMenuBar(menuBar);
        fileMenu = new JMenu("File");
        imageOpMenu = new JMenu("Image Operations");
        ccMenu = new JMenu("Connected Components");
        helpMenu = new JMenu("Help");
        menuBar.add(fileMenu);
        menuBar.add(imageOpMenu);
        menuBar.add(ccMenu);
        menuBar.add(helpMenu);

        // Create the File menu's menu items.
        loadImageItem = new JMenuItem("Load image...");
        loadImageItem.addActionListener(this);
        fileMenu.add(loadImageItem);
        saveAsItem = new JMenuItem("Save as full-color PNG");
        saveAsItem.addActionListener(this);
        fileMenu.add(saveAsItem);
        exitItem = new JMenuItem("Quit");
        exitItem.addActionListener(this);
        fileMenu.add(exitItem);

        // Create the Image Operation menu items.
        lowPassItem = new JMenuItem("Convolve with blurring kernel");
        lowPassItem.addActionListener(this);
        imageOpMenu.add(lowPassItem);
        highPassItem = new JMenuItem("Convolve with sharpening kernel");
        highPassItem.addActionListener(this);
        imageOpMenu.add(highPassItem);
        photoNegItem = new JMenuItem("Photonegative");
        photoNegItem.addActionListener(this);
        imageOpMenu.add(photoNegItem);
        RGBThreshItem = new JMenuItem("RGB Thresholds at 128");
        RGBThreshItem.addActionListener(this);
        imageOpMenu.add(RGBThreshItem);

 
        // Create CC menu stuff.
        CCItem1 = new JMenuItem("Compute Connected Components and Recolor");
        CCItem1.addActionListener(this);
        ccMenu.add(CCItem1);

        CCItem2 = new JMenuItem("Segment Image and Recolor");
        CCItem2.addActionListener(this);
        ccMenu.add(CCItem2);

        CCItem3 = new JMenuItem("Segment Image Using a Maximum Pixel DELTA");
        CCItem3.addActionListener(this);
        ccMenu.add(CCItem3);

        // Create the Help menu's item.
        aboutItem = new JMenuItem("About");
        aboutItem.addActionListener(this);
        helpMenu.add(aboutItem);
        helpItem = new JMenuItem("Help");
        helpItem.addActionListener(this);
        helpMenu.add(helpItem);

        // Initialize the image operators, if this is the first call to the constructor:
        if (PHOTONEG_OP==null) {
            byte[] lut = new byte[256];
            for (int j=0; j<256; j++) {
                lut[j] = (byte)(256-j); 
            }
            ByteLookupTable blut = new ByteLookupTable(0, lut); 
            PHOTONEG_OP = new LookupOp(blut, null);
        }
        if (RGBTHRESH_OP==null) {
            byte[] lut = new byte[256];
            for (int j=0; j<256; j++) {
                lut[j] = (byte)(j < 128 ? 0: 200);
            }
            ByteLookupTable blut = new ByteLookupTable(0, lut); 
            RGBTHRESH_OP = new LookupOp(blut, null);
        }
        if (LOWPASS_OP==null) {
            float[] data = BLURRING_KERNEL;
            LOWPASS_OP = new ConvolveOp(new Kernel(3, 3, data),
                                        ConvolveOp.EDGE_NO_OP,
                                        null);
        }
        if (HIGHPASS_OP==null) {
            float[] data = SHARPENING_KERNEL;
            HIGHPASS_OP = new ConvolveOp(new Kernel(3, 3, data),
                                        ConvolveOp.EDGE_NO_OP,
                                        null);
        }
        loadImage(startingImage); // Read in the pre-selected starting image.
        setVisible(true); // Display it.
    }
    
    /*
     * Given a path to a file on the file system, try to load in the file
     * as an image.  If that works, replace any current image by the new one.
     * Re-make the biFiltered buffered image, too, because its size probably
     * needs to be different to match that of the new image.
     */
    public void loadImage(String filename) {
        try {
            biTemp = ImageIO.read(new File(filename));
            w = biTemp.getWidth();
            h = biTemp.getHeight();
            viewPanel.setSize(w,h);
            biWorking = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            gWorking = biWorking.getGraphics();
            gWorking.drawImage(biTemp, 0, 0, null);
            biFiltered = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            pack(); // Lay out the JFrame and set its size.
            repaint();
        } catch (IOException e) {
            System.out.println("Image could not be read: "+filename);
            System.exit(1);
        }
    }

    /* Menu handlers
     */
    void handleFileMenu(JMenuItem mi){
        System.out.println("A file menu item was selected.");
        if (mi==loadImageItem) {
            File loadFile = new File("image-to-load.png");
            if (fileChooser==null) {
                fileChooser = new JFileChooser();
                fileChooser.setSelectedFile(loadFile);
                fileChooser.setFileFilter(new FileNameExtensionFilter("Image files", new String[] { "JPG", "JPEG", "GIF", "PNG" }));
            }
            int rval = fileChooser.showOpenDialog(this);
            if (rval == JFileChooser.APPROVE_OPTION) {
                loadFile = fileChooser.getSelectedFile();
                loadImage(loadFile.getPath());
            }
        }
        if (mi==saveAsItem) {
            File saveFile = new File("savedimage.png");
            fileChooser = new JFileChooser();
            fileChooser.setSelectedFile(saveFile);
            int rval = fileChooser.showSaveDialog(this);
            if (rval == JFileChooser.APPROVE_OPTION) {
                saveFile = fileChooser.getSelectedFile();
                // Save the current image in PNG format, to a file.
                try {
                    ImageIO.write(biWorking, "png", saveFile);
                } catch (IOException ex) {
                    System.out.println("There was some problem saving the image.");
                }
            }
        }
        if (mi==exitItem) { this.setVisible(false); System.exit(0); }
    }

    void handleEditMenu(JMenuItem mi){
        System.out.println("An edit menu item was selected.");
    }

    void handleImageOpMenu(JMenuItem mi){
        System.out.println("An imageOp menu item was selected.");
        if (mi==lowPassItem) { applyOp(LOWPASS_OP); }
        else if (mi==highPassItem) { applyOp(HIGHPASS_OP); }
        else if (mi==photoNegItem) { applyOp(PHOTONEG_OP); }
        else if (mi==RGBThreshItem) { applyOp(RGBTHRESH_OP); }
        repaint();
    }

    void handleCCMenu(JMenuItem mi) {
        System.out.println("A connected components menu item was selected.");
        if (mi==CCItem1) {
            System.out.println("A connected components menu item was selected.");
            computeConnectedComponents();
        }
        if (mi==CCItem2) {
            int nregions = 25; // default value.
            String inputValue = JOptionPane.showInputDialog("Please input the number of regions desired");
            try {
                nregions = (new Integer(inputValue)).intValue();
            }
            catch(Exception e) {
                System.out.println(e);
                System.out.println("That did not convert to an integer. Using the default: 25.");
            }
            System.out.println("nregions is "+nregions);
            // Call your image segmentation method here.
            createImageSegment(NREGIONS_CHECK, nregions);
        }

        if(mi==CCItem3) {
            String inputValue = JOptionPane.showInputDialog("Please input the delta desired.");
            int delta = 9;
            try {
                delta = (new Integer(inputValue)).intValue();
            }
            catch(Exception e) {
                System.out.println(e);
                System.out.println("That did not convert to an integer. Using the default: 9.");
            }
            System.out.println("delta is "+ delta);
            // Call your image segmentation method here.
            createImageSegment(DELTA_CHECK, delta);
        }
    }

    void handleHelpMenu(JMenuItem mi){
        System.out.println("A help menu item was selected.");
        if (mi==aboutItem) {
            System.out.println("About: Well this is my program.");
            JOptionPane.showMessageDialog(this,
                "Image Components, Starter-Code Version.",
                "About",
                JOptionPane.PLAIN_MESSAGE);
        }
        else if (mi==helpItem) {
            System.out.println("In case of panic attack, select File: Quit.");
            JOptionPane.showMessageDialog(this,
                "To load a new image, choose File: Load image...\nFor anything else, just try different things.",
                "Help",
                JOptionPane.PLAIN_MESSAGE);
        }
    }

    /*
     * Used by Swing to set the size of the JFrame when pack() is called.
     */
    public Dimension getPreferredSize() {
        return new Dimension(w, h+50); // Leave some extra height for the menu bar.
    }

    public void paintPanel(Graphics g) {
        g.drawImage(biWorking, 0, 0, null);
    }
            	
    public void applyOp(BufferedImageOp operation) {
        operation.filter(biWorking, biFiltered);
        gWorking.drawImage(biFiltered, 0, 0, null);
    }

    public void actionPerformed(ActionEvent e) {
        Object obj = e.getSource(); // What Swing object issued the event?
        if (obj instanceof JMenuItem) { // Was it a menu item?
            JMenuItem mi = (JMenuItem)obj; // Yes, cast it.
            JPopupMenu pum = (JPopupMenu)mi.getParent(); // Get the object it's a child of.
            JMenu m = (JMenu) pum.getInvoker(); // Get the menu from that (popup menu) object.
            //System.out.println("Selected from the menu: "+m.getText()); // Printing this is a debugging aid.

            if (m==fileMenu)    { handleFileMenu(mi);    return; }  // Handle the item depending on what menu it's from.
            if (m==imageOpMenu) { handleImageOpMenu(mi); return; }
            if (m==ccMenu)      { handleCCMenu(mi);      return; }
            if (m==helpMenu)    { handleHelpMenu(mi);    return; }
        } else {
            System.out.println("Unhandled ActionEvent: "+e.getActionCommand());
        }
    }


    // Use this to put color information into a pixel of a BufferedImage object.
    void putPixel(BufferedImage bi, int x, int y, int r, int g, int b) {
        int rgb = (r << 16) | (g << 8) | b; // pack 3 bytes into a word.
        bi.setRGB(x,  y, rgb);
    }

    /*
     * Processes the image by connecting components using union-finds under specific conditions.
     * Repaints the image at the end.
     */
    void computeConnectedComponents() {
        initializeParentID();   // Setups the uptree
        Integer count = 0;

        // Scans the entire image, connecting components if they are the same color and their roots
        // are also not the same
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int currPixelID = (y) * w + x;
                int neighborPixelID = y * w + x + 1;
                if (x != w - 1 && biWorking.getRGB(getXcoord(currPixelID), getYcoord(currPixelID))
                        == biWorking.getRGB(getXcoord(neighborPixelID), getYcoord(neighborPixelID))
                        && find(currPixelID) != find(neighborPixelID)) {
                            union(currPixelID, neighborPixelID);
                            count++;
                }

                neighborPixelID = (y + 1) * w + x;
                if (y != h - 1 && biWorking.getRGB(getXcoord(currPixelID), getYcoord(currPixelID))
                        == biWorking.getRGB(getXcoord(neighborPixelID), getYcoord(neighborPixelID))
                        && find(currPixelID) != find(neighborPixelID)) {
                    union(currPixelID, neighborPixelID);
                    count++;
                }
            }
        }

        repaintImage(count);
    }

    void createImageSegment(int checkCase, int delta) {
        PriorityQueue<Edge> edges = new PriorityQueue<Edge>();
        initializeParentID();   // Setups the uptree

        //Create the weighted pixel graph
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int currPixelID = (y) * w + x;
                int currentPixelRGB = biWorking.getRGB(getXcoord(currPixelID), getYcoord(currPixelID));
                Color currentPixelColor = new Color(currentPixelRGB >> 16 & 255, currentPixelRGB >> 8 & 255, currentPixelRGB & 255);
                if (x < w - 1) {
                    int neighborPixelID = y * w + x + 1;
                    int neighborPixelRGB = biWorking.getRGB(getXcoord(neighborPixelID), getYcoord(neighborPixelID));
                    Color neighborPixelColor = new Color(neighborPixelRGB >> 16 & 255, neighborPixelRGB >> 8 & 255, neighborPixelRGB & 255);
                    double colorDistance = currentPixelColor.euclideanDistance(neighborPixelColor);
                    int colorDistanceSqr = (int) (colorDistance * colorDistance);
                    edges.add(new Edge(currPixelID, neighborPixelID, colorDistanceSqr));
                }

                if (y < h - 1) {
                    int neighborPixelID = (y + 1) * w + x;
                    int neighborPixelRGB = biWorking.getRGB(getXcoord(neighborPixelID), getYcoord(neighborPixelID));
                    Color neighborPixelColor = new Color(neighborPixelRGB >> 16 & 255, neighborPixelRGB >> 8 & 255, neighborPixelRGB & 255);
                    double colorDistance = currentPixelColor.euclideanDistance(neighborPixelColor);
                    int colorDistanceSqr = (int) (colorDistance * colorDistance);
                    edges.add(new Edge(currPixelID, neighborPixelID, colorDistanceSqr));
                }
            }
        }
        Integer count = 0;

        switch(checkCase) {
            case DELTA_CHECK:
                if(edges.size() > 0) {
                    Edge currentEdge = edges.remove();
                    while(currentEdge != null && currentEdge.getWeight() <= delta) {
                        int pixelID1 = currentEdge.getEndpoint0();
                        int pixelID2 = currentEdge.getEndpoint1();
                        int root1 = find(pixelID1);
                        int root2 = find(pixelID2);
                        if(root1 != root2) {
                            union(pixelID1, pixelID2);
                            count++;
                        }
                        currentEdge = (edges.size() > 0) ? edges.remove() : null;
                    }
                }
                break;

            case NREGIONS_CHECK:
                int currentSegments = h * w;
                while(currentSegments > delta) {
                    Edge currentEdge = edges.remove();
                    int pixelID1 = currentEdge.getEndpoint0();
                    int pixelID2 = currentEdge.getEndpoint1();
                    int root1 = find(pixelID1);
                    int root2 = find(pixelID2);
                    if(root1 != root2) {
                        union(pixelID1, pixelID2);
                        currentSegments--;
                        count++;
                    }
                }
                break;
        }

        System.out.println("Done finding minimum spannng forest");

        repaintImage(count);
    }

        void repaintImage(Integer count) {
            System.out.println("The number of times that the method UNION was called for this image is: " + count + ".");

            HashMap<Integer, Integer> componentNumber = new HashMap<Integer, Integer>();
            count = 0;

            // Scans the entire parentID array and counts how many roots is found. That root and its count
            // is added then added to the componentNumber.
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    if (parentID[y][x] == -1) {
                        componentNumber.put(Integer.valueOf(y * w + x), count);
                        count++;
                    }
                }
            }
            System.out.println("The number of connected components in this image is: " + count + ".");

            // Scans the entire image again, but repaints the image with the count values of the roots
            // from the componentNumber
            ProgressiveColors progressiveColors = new ProgressiveColors();
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    Integer rootID = Integer.valueOf(find(y * w + x));
                    int value = componentNumber.get(rootID).intValue();
                    int[] rgb = progressiveColors.progressiveColor(value);
                    putPixel(biWorking, x, y, rgb[0], rgb[1], rgb[2]);
                }
            }
            repaint();
        }

    /* This main method can be used to run the application. */
    public static void main(String s[]) {
        appInstance = new ImageComponents();
    }
}
