package org.radrails.rails.internal.ui;

import java.io.File;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.radrails.rails.ui.RailsUIPlugin;

public class DeployWizardPage extends WizardPage
{

	private static final String HEROKU_IMG_PATH = "icons/heroku.png"; //$NON-NLS-1$

	private Button deployWithFTP;
	private Button deployWithCapistrano;
	private Button deployWithHeroku;

	protected DeployWizardPage()
	{
		super("Deployment", "Choose your deployment option", RailsUIPlugin.getImageDescriptor(HEROKU_IMG_PATH));
	}

	@Override
	public void createControl(Composite parent)
	{

		Composite composite = new Composite(parent, SWT.NULL);
		composite.setLayout(new GridLayout());
		composite.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL));
		setControl(composite);

		initializeDialogUnits(parent);

		// Actual contents
		Label label = new Label(composite, SWT.NONE);
		label.setText("Need a provider? Sign up for one here:");

		// deploy with Heroku
		deployWithHeroku = new Button(composite, SWT.RADIO);
		deployWithHeroku.setImage(RailsUIPlugin.getImage(HEROKU_IMG_PATH));
		deployWithHeroku.setSelection(true);

		label = new Label(composite, SWT.NONE);
		label.setText("or choose one of these deployment mechanisms:");

		// "Other" Deployment options radio button group
		deployWithFTP = new Button(composite, SWT.RADIO);
		deployWithFTP.setText("Deploy using FTP");

		deployWithCapistrano = new Button(composite, SWT.RADIO);
		deployWithCapistrano.setText("Deploy using Capistrano");

		Dialog.applyDialogFont(composite);
	}

	@Override
	public IWizardPage getNextPage()
	{
		IWizardPage nextPage = null;
		// Determine what page is next by the user's choice in the radio buttons
		if (deployWithHeroku.getSelection())
		{
			// Check for credentials already existing in ~/.heroku/credentials file, if they do skip to
			// HerokuDeployWizardPage
			String userHome = System.getProperty("user.home");
			if (userHome != null && userHome.trim().length() > 0)
			{
				File herokuDir = new File(userHome, ".heroku");
				if (herokuDir.isDirectory())
				{
					File credentialsFile = new File(herokuDir, "credentials");
					if (credentialsFile.exists())
					{
						nextPage = new HerokuDeployWizardPage();
					}
				}
			}
			if (nextPage == null)
				nextPage = new HerokuLoginWizardPage();
		}
		else if (deployWithFTP.getSelection())
		{
			nextPage = new FTPDeployWizardPage();
		}
		else if (deployWithCapistrano.getSelection())
		{
			// TODO CapistranoDeployWizardPage
		}
		if (nextPage == null)
			nextPage = super.getNextPage();
		if (nextPage != null)
		{
			nextPage.setWizard(getWizard());
		}
		return nextPage;
	}

	@Override
	public IWizardPage getPreviousPage()
	{
		return null;
	}

}
