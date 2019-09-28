// -*- tab-width: 4 -*-
package edu.nyu.jet.hmm;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import edu.nyu.jet.tipster.*;

/**
 *  Provides top-level control over a session involving the annotation of
 *  name tags.  Three buttons are provided:  init (initalize);
 *  learn (annotate more data);  save.
 */  
public class ActiveLearnerTool extends JFrame {

	public ActiveLearnerTool () {

		Container content = this.getContentPane();
		content.setLayout (new FlowLayout());
		this.setTitle("ActiveLearnerTool");

		JPanel panel = new JPanel();
		final JButton init = new JButton("init");
		panel.add(init);
		final JButton learn = new JButton("learn");
		panel.add(learn);
		learn.setEnabled(false);
		final JButton save = new JButton("save");
		panel.add(save);
		save.setEnabled(false);
		content.add(panel);
		pack();
		setVisible(true);

		init.addActionListener (new ActionListener() {
			public void actionPerformed (ActionEvent ev) {
				init.setEnabled(false);
				Thread initializerThread = new Thread() {
					@Override
					public void run () {
						ActiveLearner.initialize();
						learn.setEnabled(true);
					}
				};
				initializerThread.start();
			}
		});

		learn.addActionListener (new ActionListener() {
			public void actionPerformed (ActionEvent ev) {
				learn.setEnabled(false);
				ActiveLearner.keepLearning = true;
				Thread learnerThread = new Thread() {
					@Override
					public void run () {
						while (ActiveLearner.keepLearning)
							ActiveLearner.learn();
						learn.setEnabled(true);
						save.setEnabled(true);
					}
				};
				learnerThread.start();
			}
		});

		save.addActionListener (new ActionListener() {
			public void actionPerformed (ActionEvent ev) {
				System.out.println ("pushed save");
				// ActiveLearner.col.saveAs("...");
			}
		});
	}

	public static void main (String[] args) {
        if (args.length != 2) {
            System.out.println("activelearning tool requires two arguments: directory  fileList");
            System.exit(1);
        }
        String directory = args[0];
        String fileList = args[1];
		String home = System.getProperty("jetHome");
		new AnnotationColor(home + "/data");
		ActiveLearner.col = new DocumentCollection(directory, fileList);

		new ActiveLearnerTool();
	}
}
