import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.ImageCanvas;
import ij.gui.ImageWindow;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Panel;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JSlider;
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
			//Rausgenommen wegen Redundanz
			int r = 0;
			int g = 0;
			int b = 0;
			
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
						
						for (int i = -1; i <= 1; i++) {
							for (int j = -1; j <= 1; j++) {
//								Alte Logik, out of bounds!
//								if (i+y<0 || i+y>height) i=0;
//								if (j+x<0 || j+x>width) j=0;

								if (i + y >= 0 && i + y < height && j + x >= 0 && j + x < width) {

									int colors = origPixels[pos + i * width + j];
									int rn = (colors >> 16) & 0xff;
									int gn = (colors >> 8) & 0xff;
									int bn = colors & 0xff;

									r += rn / 9;
									g += gn / 9;
									b += bn / 9;
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

						for (int i = -1; i <= 1; i++) {
							for (int j = -1; j <= 1; j++) {

								if (i + y >= 0 && i + y < height && j + x >= 0 && j + x < width) {
									int colors = origPixels[pos + i * width + j];
									int rn = (colors >> 16) & 0xff;
									int gn = (colors >> 8) & 0xff;
									int bn = colors & 0xff;
									
									//Mittelpixel wird anders behandelt
									if (i == 0 && j == 0) {
										r += rn * 8 / 9;
										g += gn * 8 / 9;
										b += bn * 8 / 9;
									} else {
										r -= rn / 9;
										g -= gn / 9;
										b -= bn / 9;
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
						int argb = origPixels[pos]; // Lesen der Originalwerte

						int r = (argb >> 16) & 0xff;
						int g = (argb >> 8) & 0xff;
						int b = argb & 0xff;

						int rn = r / 2;
						int gn = g / 2;
						int bn = b / 2;

						pixels[pos] = (0xFF << 24) | (rn << 16) | (gn << 8) | bn;
					}
				}
			}

		}

	} // CustomWindow inner class
}
