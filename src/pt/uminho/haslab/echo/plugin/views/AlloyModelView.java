package pt.uminho.haslab.echo.plugin.views;

import java.awt.Frame;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.part.ViewPart;

import pt.uminho.haslab.echo.ErrorAlloy;
import pt.uminho.haslab.echo.ErrorParser;
import pt.uminho.haslab.echo.ErrorTransform;
import pt.uminho.haslab.echo.ErrorUnsupported;
import pt.uminho.haslab.echo.plugin.EchoPlugin;
import pt.uminho.haslab.echo.plugin.properties.ProjectProperties;
import edu.mit.csail.sdg.alloy4.Err;
import edu.mit.csail.sdg.alloy4compiler.translator.A4Solution;
import edu.mit.csail.sdg.alloy4viz.VizGUI;

public class AlloyModelView extends ViewPart {
  
	VizGUI viz;
	A4Solution sol;
	String pathToWrite;
	String mmURI;
	ProjectProperties pp;
	
	

	public AlloyModelView()
	{
		 super();
		 File file = new File(".dummy.xml");
			try {
				file.createNewFile();
				FileWriter fileWritter = new FileWriter(file.getName(),false);
		        BufferedWriter bufferWritter = new BufferedWriter(fileWritter);
		        bufferWritter.write("<alloy><instance bitwidth=\"0\" maxseq=\"0\"></instance></alloy>");
		        bufferWritter.close();
				viz = new VizGUI(false, ".dummy.xml", null,null,null,false);
				viz.doShowViz();

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		    EchoPlugin.getInstance().setAlloyView(this);
	}
	
	
	@Override
	public void createPartControl(Composite parent) {
		// TODO Auto-generated method stub
		
		
		
		
		Composite composite = new Composite(parent, SWT.EMBEDDED | SWT.NO_BACKGROUND);
	    Frame frame = SWT_AWT.new_Frame(composite);
	    frame.add(viz.getPanel());

	    //loadGraph();
	    
	}
	
	
	

	
	public void refresh()
	{
		loadGraph();
	}

	public void setPathToWrite(String path)
	{
		pathToWrite = path;
	}
	
	public void setProperties(ProjectProperties pp)
	{
		this.pp = pp;
	}
	

	public void setMetamodel(String mm)
	{
		mmURI = mm;
	}

	public void loadGraph()
	{
		sol = EchoPlugin.getInstance().getEchoRunner().getAInstance();
		   try {
			   if(sol != null && sol.satisfiable())
			   {
				   sol.writeXML(".dummy.xml");
				   viz.loadXML(".dummy.xml",true);
				   EchoPlugin.getInstance().getEchoRunner().generateTheme(viz.getVizState());
				   viz.doShowViz();
			   }
		   }catch (Err e) {
			   // TODO Auto-generated catch block
			   e.printStackTrace();
		   }
	}
	public void dispose(){
		EchoPlugin.getInstance().deleteView();
		super.dispose();
	}
	
	

	@Override
	public void setFocus() {
		// TODO Auto-generated method stub
		
	}
	
	public void clean () {
		File file = new File(".dummy.xml");
		try {
			file.createNewFile();
			FileWriter fileWritter = new FileWriter(file.getName(),false);
	        BufferedWriter bufferWritter = new BufferedWriter(fileWritter);
	        bufferWritter.write("<alloy><instance bitwidth=\"0\" maxseq=\"0\"></instance></alloy>");
	        bufferWritter.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		viz.loadXML(".dummy.xml", true);
		viz.doShowViz();
	}

	public void saveInstance() {
		try {
			if (EchoPlugin.getInstance().getEchoRunner().hasInstance(pathToWrite))
				EchoPlugin.getInstance().getEchoRunner().writeInstance(pathToWrite);
			else{
				System.out.println(mmURI +" , "+pathToWrite);
				EchoPlugin.getInstance().getEchoRunner().writeAllInstances(mmURI,pathToWrite);
				pp.addConformList(pathToWrite);
			}

			pathToWrite = null;
		} catch (Exception e) {
			Shell shell = this.getSite().getShell();
			MessageDialog.openInformation(shell, "Error saving update.",e.getMessage());
			e.printStackTrace();
		}
		
		
	}
}