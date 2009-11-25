/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Jakub Jurkiewicz <jakub.jurkiewicz@gmail.com> - Fix for Bug 174737
 *     [IDE] New Plug-in Project wizard status handling is inconsistent
 *     Oakland Software Incorporated (Francis Upton) <francisu@ieee.org>
 *		    Bug 224997 [Workbench] Impossible to copy project
 *******************************************************************************/
package org.radrails.rails.internal.ui;

import java.net.URI;
import java.net.URISyntaxException;

import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.ide.IIDEHelpContextIds;
import org.eclipse.ui.internal.ide.dialogs.IDEResourceInfoUtils;

/**
 * Standard main page for a wizard that is creates a project resource.
 * <p>
 * This page may be used by clients as-is; it may be also be subclassed to suit.
 * </p>
 * <p>
 * Example usage:
 * 
 * <pre>
 * mainPage = new WizardNewProjectCreationPage(&quot;basicNewProjectPage&quot;);
 * mainPage.setTitle(&quot;Project&quot;);
 * mainPage.setDescription(&quot;Create a new project resource.&quot;);
 * </pre>
 * 
 * </p>
 */
public class WizardNewProjectCreationPage extends WizardPage
{

	// initial value stores
	private String initialProjectFieldValue;

	// widgets
	private Text projectNameField;
	private StyledText locationPathField;
	private Button browseButton;
	private Button runGenerator;
	private Button gitCloneGenerate;
	private StyledText gitLocation;

	private Listener nameModifyListener = new Listener()
	{
		public void handleEvent(Event e)
		{
			setLocationForSelection();
			boolean valid = validatePage();
			setPageComplete(valid);

		}
	};

	// constants
	private static final int SIZING_TEXT_FIELD_WIDTH = 250;
	private static final String SAVED_LOCATION_ATTR = "OUTSIDE_LOCATION"; //$NON-NLS-1$
	private static final String EMPTY_STRING = ""; //$NON-NLS-1$

	/**
	 * Creates a new project creation wizard page.
	 * 
	 * @param pageName
	 *            the name of this page
	 */
	public WizardNewProjectCreationPage(String pageName)
	{
		super(pageName);
		setPageComplete(false);
	}

	/**
	 * (non-Javadoc) Method declared on IDialogPage.
	 */
	public void createControl(Composite parent)
	{
		Composite composite = new Composite(parent, SWT.NULL);

		initializeDialogUnits(parent);

		PlatformUI.getWorkbench().getHelpSystem().setHelp(composite, IIDEHelpContextIds.NEW_PROJECT_WIZARD_PAGE);

		composite.setLayout(new GridLayout());
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));

		createProjectNameGroup(composite);

		// create Location section
		createUserEntryArea(composite);

		// Add the generate app section
		createGenerateGroup(composite);

		// Scale the button based on the rest of the dialog
		setButtonLayoutData(browseButton);

		setPageComplete(validatePage());
		// Show description on opening
		setErrorMessage(null);
		setMessage(null);
		setControl(composite);
		Dialog.applyDialogFont(composite);
	}

	/**
	 * Create the area for user entry.
	 * 
	 * @param composite
	 * @param defaultEnabled
	 */
	private void createUserEntryArea(Composite composite)
	{
		// project specification group
		Composite projectGroup = new Composite(composite, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 4;
		projectGroup.setLayout(layout);
		projectGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		// location label
		Label locationLabel = new Label(projectGroup, SWT.NONE);
		locationLabel.setText(Messages.ProjectLocationSelectionDialog_locationLabel);

		// project location entry field
		locationPathField = new StyledText(projectGroup, SWT.BORDER);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.widthHint = SIZING_TEXT_FIELD_WIDTH;
		data.horizontalSpan = 2;
		locationPathField.setLayoutData(data);

		// browse button
		browseButton = new Button(projectGroup, SWT.PUSH);
		browseButton.setText(Messages.WizardNewProjectCreationPage_BrowseLabel);
		browseButton.addSelectionListener(new SelectionAdapter()
		{
			public void widgetSelected(SelectionEvent event)
			{
				handleLocationBrowseButtonPressed();
			}
		});

		if (initialProjectFieldValue == null)
		{
			locationPathField.setText(EMPTY_STRING);
		}
		else
		{
			locationPathField.setText(initialProjectFieldValue);
		}

		locationPathField.addModifyListener(new ModifyListener()
		{
			/*
			 * (non-Javadoc)
			 * @see org.eclipse.swt.events.ModifyListener#modifyText(org.eclipse.swt.events.ModifyEvent)
			 */
			public void modifyText(ModifyEvent e)
			{
				reportError(checkValidLocation(), false);
			}
		});
	}

	/**
	 * Return the path on the location field.
	 * 
	 * @return String
	 */
	private String getPathFromLocationField()
	{
		URI fieldURI;
		try
		{
			fieldURI = new URI(locationPathField.getText());
		}
		catch (URISyntaxException e)
		{
			return locationPathField.getText();
		}
		return fieldURI.getPath();
	}

	/**
	 * Open an appropriate directory browser
	 */
	protected void handleLocationBrowseButtonPressed()
	{
		String selectedDirectory = null;
		String dirName = getPathFromLocationField();

		if (!dirName.equals(EMPTY_STRING))
		{
			IFileInfo info = IDEResourceInfoUtils.getFileInfo(dirName);

			if (info == null || !(info.exists()))
				dirName = EMPTY_STRING;
		}
		else
		{
			String value = getDialogSettings().get(SAVED_LOCATION_ATTR);
			if (value != null)
			{
				dirName = value;
			}
		}

		DirectoryDialog dialog = new DirectoryDialog(locationPathField.getShell(), SWT.SHEET);
		dialog.setMessage(EMPTY_STRING);
		dialog.setFilterPath(dirName);
		selectedDirectory = dialog.open();

		if (selectedDirectory != null)
		{
			locationPathField.setText(selectedDirectory);
			getDialogSettings().put(SAVED_LOCATION_ATTR, selectedDirectory);
		}

	}

	protected String checkValidLocation()
	{
		// TODO Auto-generated method stub
		return null;
	}

	protected void reportError(String errorMessage, boolean infoOnly)
	{
		if (infoOnly)
		{
			setMessage(errorMessage, IStatus.INFO);
			setErrorMessage(null);
		}
		else
			setErrorMessage(errorMessage);
		boolean valid = errorMessage == null;
		if (valid)
		{
			valid = validatePage();
		}

		setPageComplete(valid);
	}

	/**
	 * Creates the project name specification controls.
	 * 
	 * @param parent
	 *            the parent composite
	 */
	private final void createProjectNameGroup(Composite parent)
	{
		// project specification group
		Composite projectGroup = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		projectGroup.setLayout(layout);
		projectGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		// new project label
		Label projectLabel = new Label(projectGroup, SWT.NONE);
		projectLabel.setText(Messages.WizardNewProjectCreationPage_nameLabel);
		projectLabel.setFont(parent.getFont());

		// new project name entry field
		projectNameField = new Text(projectGroup, SWT.BORDER);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.widthHint = SIZING_TEXT_FIELD_WIDTH;
		projectNameField.setLayoutData(data);
		projectNameField.setFont(parent.getFont());

		// Set the initial value first before listener
		// to avoid handling an event during the creation.
		if (initialProjectFieldValue != null)
		{
			projectNameField.setText(initialProjectFieldValue);
		}
		projectNameField.addListener(SWT.Modify, nameModifyListener);
	}

	/**
	 * Creates the project generation controls.
	 * 
	 * @param parent
	 *            the parent composite
	 */
	private final void createGenerateGroup(Composite parent)
	{
		// project generation group
		Group group = new Group(parent, SWT.BORDER);
		group.setLayout(new GridLayout(1, false));
		group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		group.setText(Messages.WizardNewProjectCreationPage_GenerateAppGroupLabel);

		runGenerator = new Button(group, SWT.RADIO);
		runGenerator.setText(Messages.WizardNewProjectCreationPage_StandardGeneratorText);
		runGenerator.setSelection(true);

		gitCloneGenerate = new Button(group, SWT.RADIO);
		gitCloneGenerate.setText("Clone an existing git project:");

		// project specification group
		Composite projectGroup = new Composite(group, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 4;
		projectGroup.setLayout(layout);
		projectGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		// location label
		Label locationLabel = new Label(projectGroup, SWT.NONE);
		locationLabel.setText(Messages.ProjectLocationSelectionDialog_locationLabel);

		// project location entry field
		gitLocation = new StyledText(projectGroup, SWT.BORDER);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.widthHint = SIZING_TEXT_FIELD_WIDTH;
		data.horizontalSpan = 2;
		gitLocation.setLayoutData(data);

		// browse button
		Button gitBrowseButton = new Button(projectGroup, SWT.PUSH);
		gitBrowseButton.setText(Messages.WizardNewProjectCreationPage_BrowseLabel);

		Button noGenerator = new Button(group, SWT.RADIO);
		noGenerator.setText(Messages.WizardNewProjectCreationPage_NoGeneratorText);
	}

	/**
	 * Returns the current project location path as entered by the user, or its anticipated initial value. Note that if
	 * the default has been returned the path in a project description used to create a project should not be set.
	 * 
	 * @return the project location path or its anticipated initial value.
	 */
	public IPath getLocationPath()
	{
		return new Path(locationPathField.getText());
	}

	/**
	 * /** Returns the current project location URI as entered by the user, or <code>null</code> if a valid project
	 * location has not been entered.
	 * 
	 * @return the project location URI, or <code>null</code>
	 * @since 3.2
	 */
	public URI getLocationURI()
	{
		return getLocationPath().toFile().toURI();
	}

	/**
	 * Creates a project resource handle for the current project name field value. The project handle is created
	 * relative to the workspace root.
	 * <p>
	 * This method does not create the project resource; this is the responsibility of <code>IProject::create</code>
	 * invoked by the new project resource wizard.
	 * </p>
	 * 
	 * @return the new project resource handle
	 */
	public IProject getProjectHandle()
	{
		return ResourcesPlugin.getWorkspace().getRoot().getProject(getProjectName());
	}

	/**
	 * Returns the current project name as entered by the user, or its anticipated initial value.
	 * 
	 * @return the project name, its anticipated initial value, or <code>null</code> if no project name is known
	 */
	public String getProjectName()
	{
		if (projectNameField == null)
		{
			return initialProjectFieldValue;
		}

		return getProjectNameFieldValue();
	}

	/**
	 * Returns the value of the project name field with leading and trailing spaces removed.
	 * 
	 * @return the project name in the field
	 */
	private String getProjectNameFieldValue()
	{
		if (projectNameField == null)
		{
			return EMPTY_STRING;
		}

		return projectNameField.getText().trim();
	}

	/**
	 * Sets the initial project name that this page will use when created. The name is ignored if the
	 * createControl(Composite) method has already been called. Leading and trailing spaces in the name are ignored.
	 * Providing the name of an existing project will not necessarily cause the wizard to warn the user. Callers of this
	 * method should first check if the project name passed already exists in the workspace.
	 * 
	 * @param name
	 *            initial project name for this page
	 * @see IWorkspace#validateName(String, int)
	 */
	public void setInitialProjectName(String name)
	{
		if (name == null)
		{
			initialProjectFieldValue = null;
		}
		else
		{
			initialProjectFieldValue = name.trim();
			updateProjectName(name.trim());
		}
	}

	private void updateProjectName(String trim)
	{
		String workspace = Platform.getLocation().toOSString();
		// Only update if the location field is empty, or is the "default" value
		if (locationPathField.getText().trim().length() == 0
				|| locationPathField.getText().trim().startsWith(workspace))
		{
			String string = Platform.getLocation().append(trim).toOSString();
			locationPathField.setText(string);
			locationPathField.setStyleRange(new StyleRange(0, string.length(), getShell().getDisplay().getSystemColor(
					SWT.COLOR_DARK_GRAY), null, SWT.ITALIC));
		}
		locationPathField.setStyleRange(new StyleRange());
	}

	/**
	 * Set the location to the default location
	 */
	void setLocationForSelection()
	{
		updateProjectName(getProjectNameFieldValue());
	}

	/**
	 * Returns whether this page's controls currently all contain valid values.
	 * 
	 * @return <code>true</code> if all controls are valid, and <code>false</code> if at least one is invalid
	 */
	protected boolean validatePage()
	{
		IWorkspace workspace = ResourcesPlugin.getWorkspace();

		String projectFieldContents = getProjectNameFieldValue();
		if (projectFieldContents.equals(EMPTY_STRING))
		{
			setErrorMessage(null);
			setMessage(Messages.WizardNewProjectCreationPage_projectNameEmpty);
			return false;
		}

		IStatus nameStatus = workspace.validateName(projectFieldContents, IResource.PROJECT);
		if (!nameStatus.isOK())
		{
			setErrorMessage(nameStatus.getMessage());
			return false;
		}

		IProject handle = getProjectHandle();
		if (handle.exists())
		{
			setErrorMessage(Messages.WizardNewProjectCreationPage_projectExistsMessage);
			return false;
		}

		String validLocationMessage = checkValidLocation();
		if (validLocationMessage != null)
		{ // there is no destination location given
			setErrorMessage(validLocationMessage);
			return false;
		}

		setErrorMessage(null);
		setMessage(null);
		return true;
	}

	/*
	 * see @DialogPage.setVisible(boolean)
	 */
	public void setVisible(boolean visible)
	{
		super.setVisible(visible);
		if (visible)
		{
			projectNameField.setFocus();
		}
	}

	public boolean runGenerator()
	{
		return runGenerator.getSelection();
	}

	public boolean cloneFromGit()
	{
		return gitCloneGenerate.getSelection();
	}

	public String gitCloneURI()
	{
		return gitLocation.getText().trim();
	}

	public boolean locationIsDefault()
	{
		String defaultLocation = Platform.getLocation().append(getProjectNameFieldValue()).toOSString();
		return getLocationPath().toOSString().equals(defaultLocation);
	}
}