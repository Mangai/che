/*
 * Copyright (c) 2012-2018 Red Hat, Inc.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */
package org.eclipse.che.selenium.dashboard.workspaces.details;

import static org.eclipse.che.selenium.pageobject.dashboard.workspaces.WorkspaceDetails.WorkspaceDetailsTab.OVERVIEW;
import static org.eclipse.che.selenium.pageobject.dashboard.workspaces.WorkspaceDetails.WorkspaceDetailsTab.SERVERS;
import static org.eclipse.che.selenium.pageobject.dashboard.workspaces.WorkspaceDetails.WorkspaceDetailsTab.SSH;
import static org.eclipse.che.selenium.pageobject.dashboard.workspaces.WorkspaceDetails.WorkspaceDetailsTab.VOLUMES;
import static org.testng.Assert.assertTrue;

import com.google.inject.Inject;
import org.eclipse.che.selenium.core.client.TestWorkspaceServiceClient;
import org.eclipse.che.selenium.core.user.DefaultTestUser;
import org.eclipse.che.selenium.core.workspace.InjectTestWorkspace;
import org.eclipse.che.selenium.core.workspace.TestWorkspace;
import org.eclipse.che.selenium.pageobject.dashboard.Dashboard;
import org.eclipse.che.selenium.pageobject.dashboard.workspaces.WorkspaceDetails;
import org.eclipse.che.selenium.pageobject.dashboard.workspaces.WorkspaceOverview;
import org.eclipse.che.selenium.pageobject.dashboard.workspaces.WorkspaceServers;
import org.eclipse.che.selenium.pageobject.dashboard.workspaces.WorkspaceSsh;
import org.eclipse.che.selenium.pageobject.dashboard.workspaces.Workspaces;
import org.eclipse.che.selenium.pageobject.dashboard.workspaces.WorkspacesVolumes;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/** @author Skoryk Serhii */
public class WorkspaceDetailsSingleMachineTest {

  private String workspaceName;

  @InjectTestWorkspace(startAfterCreation = false)
  private TestWorkspace testWorkspace;

  @Inject private DefaultTestUser testUser;
  @Inject private Dashboard dashboard;
  @Inject private WorkspaceDetails workspaceDetails;
  @Inject private TestWorkspaceServiceClient workspaceServiceClient;
  @Inject private Workspaces workspaces;
  @Inject private WorkspaceServers workspaceServers;
  @Inject private WorkspaceOverview workspaceOverview;
  @Inject private WorkspaceSsh workspaceSsh;
  @Inject private WorkspacesVolumes workspacesVolumes;

  @BeforeClass
  public void setUp() throws Exception {
    workspaceName = testWorkspace.getName();

    dashboard.open();
    dashboard.selectWorkspacesItemOnDashboard();
    dashboard.waitToolbarTitleName("Workspaces");
    workspaces.selectWorkspaceItemName(workspaceName);
    workspaceDetails.waitToolbarTitleName(workspaceName);
    workspaceDetails.selectTabInWorkspaceMenu(OVERVIEW);
  }

  @AfterClass
  public void tearDown() throws Exception {
    workspaceServiceClient.delete(workspaceName, testUser.getName());
  }

  @Test
  public void checkOverviewTab() {
    workspaceOverview.checkNameWorkspace(workspaceName);
    workspaceOverview.isDeleteWorkspaceButtonExists();

    // check the Export feature
    workspaceOverview.clickExportWorkspaceBtn();
    workspaceOverview.waitClipboardWorkspaceJsonFileBtn();
    workspaceOverview.waitDownloadWorkspaceJsonFileBtn();
    workspaceOverview.clickOnHideWorkspaceJsonFileBtn();
  }

  @Test
  public void checkWorkingWithServers() {
    workspaceDetails.selectTabInWorkspaceMenu(SERVERS);

    // add a new server, save changes and check it exists
    createServer("agen", "8083", "https");

    // edit the server and check it exists
    workspaceServers.clickOnEditServerButton("agen");
    workspaceServers.enterReference("agent");
    workspaceServers.enterPort("83");
    workspaceServers.enterProtocol("http");
    workspaceDetails.clickOnUpdateButtonInDialogWindow();
    workspaceServers.checkServerExists("agent", "83");

    // delete the server and check it is not exist
    workspaceServers.clickOnDeleteServerButton("agent");
    workspaceDetails.clickOnDeleteButtonInDialogWindow();
    clickOnSaveButton();
    workspaceServers.checkServerIsNotExists("agent", "83");

    // add a new server which will be checked after the workspace staring
    createServer("agent", "8082", "https");
  }

  @Test
  public void checkSshTab() {
    workspaceDetails.selectTabInWorkspaceMenu(SSH);

    // check ssh key exist
    Assert.assertTrue(workspaceSsh.isPrivateKeyExists());
    Assert.assertTrue(workspaceSsh.isPublicKeyExists());

    // remove ssh key
    workspaceSsh.clickOnRemoveDefaultSshKeyButton();
    workspaceSsh.waitSshKeyNotExists();

    // generate ssh key
    workspaceSsh.clickOnGenerateButton();
    Assert.assertTrue(workspaceSsh.isPrivateKeyExists());
    Assert.assertTrue(workspaceSsh.isPublicKeyExists());
  }

  @Test
  public void checkVolumesTab() {
    String volumeName = "prj";
    String volumePath = "/" + volumeName;
    String renamedVolumeName = "project";
    String renamedVolumePath = "/" + renamedVolumeName;

    workspaceDetails.selectTabInWorkspaceMenu(VOLUMES);

    // create volume
    workspacesVolumes.clickOnAddVolumeButton();
    workspacesVolumes.enterVolumeName(volumeName);
    workspacesVolumes.enterVolumePath(volumePath);
    workspaceDetails.clickOnAddButtonInDialogWindow();
    assertTrue(workspacesVolumes.checkVolumeExists(volumeName, volumePath));
    clickOnSaveButton();

    // edit volume
    workspacesVolumes.clickOnEditVolumeButton(volumeName);
    workspacesVolumes.enterVolumeName(renamedVolumeName);
    workspacesVolumes.enterVolumePath(renamedVolumePath);
    workspaceDetails.clickOnUpdateButtonInDialogWindow();
    assertTrue(workspacesVolumes.checkVolumeExists(renamedVolumeName, renamedVolumePath));
    clickOnSaveButton();

    // remove volume
    workspacesVolumes.clickOnRemoveVolumeButton(renamedVolumeName);
    workspaceDetails.clickOnDeleteButtonInDialogWindow();
    workspacesVolumes.waitVolumeNotExists(renamedVolumeName);
    clickOnSaveButton();
  }

  private void clickOnSaveButton() {
    workspaceDetails.clickOnSaveChangesBtn();
    dashboard.waitNotificationMessage("Workspace updated");
    dashboard.waitNotificationIsClosed();
  }

  private void createServer(String serverName, String serverPort, String serverProtocol) {
    workspaceServers.clickOnAddServerButton();
    workspaceServers.waitAddServerDialogIsOpen();
    workspaceServers.enterReference(serverName);
    workspaceServers.enterPort(serverPort);
    workspaceServers.enterProtocol(serverProtocol);
    workspaceDetails.clickOnAddButtonInDialogWindow();
    clickOnSaveButton();
    workspaceServers.checkServerExists(serverName, serverPort);
  }
}
