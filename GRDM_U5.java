import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.ImageCanvas;
import ij.gui.ImageWindow;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Panel;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * Opens an image window and adds a panel below the image
 */
public class GRDM_U5_s0577683 implements PlugIn {

	ImagePlus imp; // ImagePlus object
	private int[] origPixels;
	private int width;
	private int height;
	
	private JTextField kernelInput;

	String[] items = { "Original", "Weichzeichnung", "Hochpassfilter", "Verstärkte Kanten" };

	public static void main(String args[]) {

		IJ.open("sail.jpg");
		// IJ.open("Z:/Pictures/Beispielbilder/orchid.jpg");

		GRDM_U5_s0577683 pw = new GRDM_U5_s0577683();
		pw.imp = IJ.getImage();
		pw.run("");
	}

	public void run(String arg) {
		if (imp == null)
			imp = WindowManager.getCurrentImage();
		if (imp == null) {
			return;
		}
		CustomCanvas cc = new CustomCanvas(imp);

		storePixelValues(imp.getProcessor());

		new CustomWindow(imp, cc);
	}

	private void storePixelValues(ImageProcessor ip) {
		width = ip.getWidth();
		height = ip.getHeight();

		origPixels = ((int[]) ip.getPixels()).clone();
	}

	class CustomCanvas extends ImageCanvas {

		CustomCanvas(ImagePlus imp) {
			super(imp);
		}

	} // CustomCanvas inner class

	class CustomWindow extends ImageWindow implements ItemListener {

		private String method;

		CustomWindow(ImagePlus imp, ImageCanvas ic) {
			super(imp, ic);
			addPanel();
		}

		void addPanel() {
			// JPanel panel = new JPanel();
			Panel panel = new Panel();

			JComboBox cb = new JComboBox(items);
			panel.add(cb);
			cb.addItemListener(this);
			
			JLabel kernelLabel = new JLabel("Kernelsize");
			panel.add(kernelLabel);

			kernelInput = new JTextField(1);
			panel.add(kernelInput);
			
			JLabel info = new JLabel("(<- wenn leer, ist die Kernelgröße 3x3)");
			panel.add(info);
			
			add(panel);
			pack();
		}

		public void itemStateChanged(ItemEvent evt) {

			// Get the affected item
			Object item = evt.getItem();

			if (evt.getStateChange() == ItemEvent.SELECTED) {
				System.out.println("Selected: " + item.toString());
				method = item.toString();
				changePixelValues(imp.getProcessor());
				imp.updateAndDraw();
			}

		}

		private void changePixelValues(ImageProcessor ip) {
			
			//Kernel-Größe auslesen, hier immer quadratisch! Eine Seite reicht
			int k = 0;
			if (!kernelInput.getText().equals("")) {
				k = Integer.parseInt(kernelInput.getText());
			}
			//Falls Kernel kleiner als 3 oder gerade, nächstgrößeren ungeraden Kernel nehmen.
			while (k<3 || k%2==0) k++;
			
			// Array zum Zurückschreiben der Pixelwerte
			int[] pixels = (int[]) ip.getPixels();

			if (method.equals("Original")) {

				for (int y = 0; y < height; y++) {
					for (int x = 0; x < width; x++) {
						int pos = y * width + x;

						pixels[pos] = origPixels[pos];
					}
				}
			}

			if (method.equals("Weichzeichnung")) {

				for (int y = 0; y < height; y++) {
					for (int x = 0; x < width; x++) {
						int pos = y * width + x;
						
						int r = 0;
						int g = 0;
						int b = 0;
						
						//Loop abhängig von Kernel-Größe
						for (int i = -(k-1)/2; i <= (k-1)/2; i++) {
							for (int j = -(k-1)/2; j <= (k-1)/2; j++) {
								
//								Alte Logik, out of bounds!
//								if (i+y<0 || i+y>height) i=0;
//								if (j+x<0 || j+x>width) j=0;
								
								if (i + y >= 0 && i + y < height && j + x >= 0 && j + x < width) {

									int colors = origPixels[pos + i * width + j];
									int rn = (colors >> 16) & 0xff;
									int gn = (colors >> 8) & 0xff;
									int bn = colors & 0xff;

									//Divisor ist die Menge der Pixel im Kernel, quadratisch - hoch 2
									r += rn / Math.pow(k, 2);
									g += gn / Math.pow(k, 2);
									b += bn / Math.pow(k, 2);
								} else {
									r += 0;
								}
							}
						}

						pixels[pos] = (0xFF << 24) | (r << 16) | (g << 8) | b;
					}
				}
			}

			if (method.equals("Hochpassfilter")) {

				for (int y = 0; y < height; y++) {
					for (int x = 0; x < width; x++) {
						int pos = y * width + x;
						
						int r = 0;
						int g = 0;
						int b = 0;

						for (int i = -(k-1)/2; i <= (k-1)/2; i++) {
							for (int j = -(k-1)/2; j <= (k-1)/2; j++) {

								if (i + y >= 0 && i + y < height && j + x >= 0 && j + x < width) {
									int colors = origPixels[pos + i * width + j];
									int rn = (colors >> 16) & 0xff;
									int gn = (colors >> 8) & 0xff;
									int bn = colors & 0xff;
									
									//Mittelpixel wird anders behandelt
									if (i == 0 && j == 0) {
										r += rn * (Math.pow(k, 2)-1) / Math.pow(k, 2);
										g += gn * (Math.pow(k, 2)-1) / Math.pow(k, 2);
										b += bn * (Math.pow(k, 2)-1) / Math.pow(k, 2);
									} else {
										r -= rn / Math.pow(k, 2);
										g -= gn / Math.pow(k, 2);
										b -= bn / Math.pow(k, 2);
									}
									
									//Bild fast schwarz, offset dazu
									r += 128;
									g += 128;
									b += 128;
								}
							}
						}

						pixels[pos] = (0xFF << 24) | (r << 16) | (g << 8) | b;
					}
				}
			}

			if (method.equals("Verstärkte Kanten")) {

				for (int y = 0; y < height; y++) {
					for (int x = 0; x < width; x++) {
						int pos = y * width + x;
						
						int r = 0;
						int g = 0;
						int b = 0;

						for (int i = -(k-1)/2; i <= (k-1)/2; i++) {
							for (int j = -(k-1)/2; j <= (k-1)/2; j++) {

								if (i + y >= 0 && i + y < height && j + x >= 0 && j + x < width) {
									int colors = origPixels[pos + i * width + j];
									int rn = (colors >> 16) & 0xff;
									int gn = (colors >> 8) & 0xff;
									int bn = colors & 0xff;
									
									//Mittelpixel wird anders behandelt
									if (i == 0 && j == 0) {
										r += rn + rn * (Math.pow(k, 2)-1) / Math.pow(k, 2);
										g += gn + gn * (Math.pow(k, 2)-1) / Math.pow(k, 2);
										b += bn + bn * (Math.pow(k, 2)-1) / Math.pow(k, 2);
									} else {
										r -= rn / Math.pow(k, 2);
										g -= gn / Math.pow(k, 2);
										b -= bn / Math.pow(k, 2);
									}
									//Farbüberlauf vorbeugen
									r = Math.min(255, Math.max(0, r));
									g = Math.min(255, Math.max(0, g));
									b = Math.min(255, Math.max(0, b));
								}
							}
						}

						pixels[pos] = (0xFF << 24) | (r << 16) | (g << 8) | b;
					}
				}
			}

		}

	} // CustomWindow inner class
}
