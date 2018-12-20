/*
 * To the extent possible under law, the ImageJ developers have waived
 * all copyright and related or neighboring rights to this tutorial code.
 *
 * See the CC0 1.0 Universal license for details:
 *     http://creativecommons.org/publicdomain/zero/1.0/
 */

package com.adaptiveenergy.imagej;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.LayoutStyle.ComponentPlacement;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.gui.Roi;
import ij.gui.RoiListener;
import ij.gui.ShapeRoi;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;
import ij.gui.ProfilePlot;
import ij.plugin.filter.*;

public class Spatial_Resolution implements PlugInFilter {
	protected ImagePlus image;

	// image property members
	private int width;
	private int height;

	// plugin parameters
	public double value;
	public String name;

	@Override
	public int setup(String arg, ImagePlus imp) {
		if (arg.equals("about")) {
			showAbout();
			return DONE;
		}

		image = imp;
		return DOES_8G | DOES_16 | DOES_32 | DOES_RGB;
	}

	@Override
	public void run(ImageProcessor ip) {
		// get width and height
		width = ip.getWidth();
		height = ip.getHeight();

		new SPWindow(image,ip);
	}

	public void showAbout() {
		IJ.showMessage("Spatial Resolution Wizard"
		);
	}

}

class SPWindow extends JFrame{
	private JPanel contentPane;
	private JLabel lblNewLabel;
	ProfilePlot Profiler;
	
	ImagePlus imp;
	private ImageProcessor ip;
	
	public SPWindow(ImagePlus image, ImageProcessor ip) {
		this.ip = ip;
		this.imp = image;		
		
		setTitle("Spatial Resolution");
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setBounds(100, 100, 606, 221);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		
		JLabel lblCreatARectangle = new JLabel("Draw a wide straight line covers the duplex wires");		
		JLabel lblSpatialResolution = new JLabel("Spatial resolution: ");		
		lblNewLabel = new JLabel("");		
		JButton btnDoneSelection = new JButton("Done Selection");
		btnDoneSelection.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(imp.getRoi()!=null) {			
					if(imp.getRoi().getType()==Roi.LINE) {
						Profiler = new ProfilePlot(imp);
						updateROI();
					}
				}		
			}
		});
		GroupLayout groupLayout = new GroupLayout(getContentPane());
		groupLayout.setHorizontalGroup(
				groupLayout.createParallelGroup(Alignment.LEADING)
					.addGroup(groupLayout.createSequentialGroup()
						.addGap(38)
						.addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
							.addComponent(lblCreatARectangle, GroupLayout.DEFAULT_SIZE, 488, Short.MAX_VALUE)
							.addGroup(groupLayout.createParallelGroup(Alignment.TRAILING, false)
								.addGroup(Alignment.LEADING, groupLayout.createSequentialGroup()
									.addComponent(lblSpatialResolution, GroupLayout.PREFERRED_SIZE, 124, GroupLayout.PREFERRED_SIZE)
									.addGap(18)
									.addComponent(lblNewLabel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
								.addGroup(Alignment.LEADING, groupLayout.createSequentialGroup()
									.addGap(356)
									.addComponent(btnDoneSelection, GroupLayout.PREFERRED_SIZE, 135, GroupLayout.PREFERRED_SIZE))))
						.addContainerGap())
			);
			groupLayout.setVerticalGroup(
				groupLayout.createParallelGroup(Alignment.LEADING)
					.addGroup(groupLayout.createSequentialGroup()
						.addGap(30)
						.addComponent(lblCreatARectangle)
						.addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
							.addGroup(groupLayout.createSequentialGroup()
								.addGap(86)
								.addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
									.addComponent(lblSpatialResolution)
									.addComponent(lblNewLabel)))
							.addGroup(groupLayout.createSequentialGroup()
								.addGap(18)
								.addComponent(btnDoneSelection)))
						.addContainerGap(48, Short.MAX_VALUE))
			);
		getContentPane().setLayout(groupLayout);
		
		if(imp.getRoi()!=null) {			
			if(imp.getRoi().getType()==Roi.LINE) {
				Profiler = new ProfilePlot(imp);
				updateROI();
			}
		}		
		setVisible(true);
	}

	private void updateROI() {
		// TODO Auto-generated method stub		
		double[] profile = Profiler.getProfile();
		Profiler.createWindow();
		MaximumFinder maxfinder = new MaximumFinder();
		int[] maxima = maxfinder.findMaxima(profile, 200.0, true);
		int[] minima = maxfinder.findMinima(profile, 200.0, true);
		int[] sortminima = minima.clone();
		Arrays.sort(sortminima);
		int[] sortmaxima = maxima.clone();
		Arrays.sort(sortmaxima);
		if(sortminima.length>=2) {
			int interval = Math.min(sortminima[sortminima.length-1]-sortminima[sortminima.length-2], sortminima[1]-sortminima[0]);
			if((sortminima[1]-sortminima[0])>(sortminima[sortminima.length-1]-sortminima[sortminima.length-2])) {
				int i;
				String sp = "";
				for(i=0;sortminima[sortminima.length-1-i]-sortminima[sortminima.length-2-i]<=(interval+5);i=i+2) {
					double[] dip = Arrays.copyOfRange(profile, sortminima[sortminima.length-2-i], sortminima[sortminima.length-1-i]);
					int[] dipcorr = maxfinder.findMaxima(dip, 200.0, true);
					//double[] peak = Arrays.copyOfRange(profile,sortminima[sortminima.length-1-i],profile.length);
					//int[] peakcorr = maxfinder.findMaxima(peak, 200.0, true);			
					if(dipcorr.length>0) {
						double resolution = (profile[dipcorr[0]+sortminima[sortminima.length-2-i]]-profile[sortminima[sortminima.length-2-i]])/(profile[maxima[0]]-profile[sortminima[sortminima.length-2-i]]);
						sp = sp + "D" +String.valueOf(i/2+1) +": " + String.valueOf(round(resolution,2)) +"  ";
					}			
					else {
						double dipcorrvalue = Arrays.stream(dip).max().getAsDouble();
						double resolution = (dipcorrvalue-profile[sortminima[sortminima.length-2-i]])/(profile[maxima[0]]-profile[sortminima[sortminima.length-2-i]]);
						sp = sp + "D" +String.valueOf(i/2+1) +": " + String.valueOf(round(resolution,2)) +"  ";
					}
					if(sortminima.length<i+2) {
						break;
					}
				}
				lblNewLabel.setText(sp);
			}
			else {
				int i=0;
				String sp = "";
				boolean flag = false;
				for(i=0;sortminima[1+i]-sortminima[i]<=interval;i=i+2) {
					double[] dip = Arrays.copyOfRange(profile, sortminima[i], sortminima[1+i]);
					int[] dipcorr = maxfinder.findMaxima(dip, 200.0, true);
					//int[] peak = maxfinder.findMaxima(Arrays.copyOfRange(profile,sortminima[1+i],profile.length), 200.0, true);
					if(dipcorr.length>0) {
						double resolution = (profile[dipcorr[0]+sortminima[i]]-profile[sortminima[i]])/(profile[maxima[0]]-profile[sortminima[i]]);
						sp = sp + "D" +String.valueOf(i/2+1) +": " + String.valueOf(round(resolution,2)) +"  ";
					}
					else {
						double dipcorrvalue = Arrays.stream(dip).max().getAsDouble();
						double resolution = (dipcorrvalue-profile[sortminima[i]])/(profile[maxima[0]]-profile[sortminima[i]]);
						sp = sp + "D" +String.valueOf(i/2+1) +": " + String.valueOf(round(resolution,2)) +"  ";
					}
					if(sortminima.length<i+2) {
						break;
					}
				}
				lblNewLabel.setText(sp);
			}	
		}
		else {
			lblNewLabel.setText("Reselect Needed");
		}
		
	}

	private void deleteROI() {
		// TODO Auto-generated method stub
		lblNewLabel.setText(null);
	}
	
	public static double round(double value, int places) {
	    if (places < 0) throw new IllegalArgumentException();

	    BigDecimal bd = new BigDecimal(value);
	    bd = bd.setScale(places, RoundingMode.HALF_UP);
	    return bd.doubleValue();
	}
	
}
