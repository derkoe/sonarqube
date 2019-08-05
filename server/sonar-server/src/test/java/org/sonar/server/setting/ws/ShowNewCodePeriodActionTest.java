/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.setting.ws;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.core.util.UuidFactoryFast;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.BranchType;
import org.sonar.db.component.ComponentDbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.newcodeperiod.NewCodePeriodDao;
import org.sonar.db.newcodeperiod.NewCodePeriodDbTester;
import org.sonar.db.newcodeperiod.NewCodePeriodDto;
import org.sonar.db.newcodeperiod.NewCodePeriodType;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.component.TestComponentFinder;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.Settings;
import org.sonarqube.ws.Settings.ShowNewCodePeriodResponse;

import static org.assertj.core.api.Assertions.assertThat;

public class ShowNewCodePeriodActionTest {
  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private ComponentDbTester componentDb = new ComponentDbTester(db);
  private DbClient dbClient = db.getDbClient();
  private DbSession dbSession = db.getSession();
  private ComponentFinder componentFinder = TestComponentFinder.from(db);
  private NewCodePeriodDao dao = new NewCodePeriodDao(System2.INSTANCE, UuidFactoryFast.getInstance());
  private NewCodePeriodDbTester tester = new NewCodePeriodDbTester(db);
  private ShowNewCodePeriodAction underTest = new ShowNewCodePeriodAction(dbClient, userSession, componentFinder, dao);
  private WsActionTester ws = new WsActionTester(underTest);

  @Test
  public void test_definition() {
    WebService.Action definition = ws.getDef();

    assertThat(definition.key()).isEqualTo("show_new_code_period");
    assertThat(definition.isInternal()).isFalse();
    assertThat(definition.since()).isEqualTo("8.0");
    assertThat(definition.isPost()).isFalse();

    assertThat(definition.params()).extracting(WebService.Param::key).containsOnly("project", "branch");
    assertThat(definition.param("project").isRequired()).isFalse();
    assertThat(definition.param("branch").isRequired()).isFalse();
  }

  @Test
  public void throw_IAE_if_branch_is_specified_without_project() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("If branch key is specified, project key needs to be specified too");

    ws.newRequest()
      .setParam("branch", "branch")
      .execute();
  }

  @Test
  public void throw_NFE_if_project_not_found() {
    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("Component key 'unknown' not found");

    ws.newRequest()
      .setParam("project", "unknown")
      .execute();
  }

  @Test
  public void throw_NFE_if_branch_not_found() {
    ComponentDto project = componentDb.insertMainBranch();
    logInAsProjectAdministrator(project);
    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("Component '" + project.getKey() + "' on branch 'unknown' not found");

    ws.newRequest()
      .setParam("project", project.getKey())
      .setParam("branch", "unknown")
      .execute();
  }

  @Test
  public void throw_IAE_if_branch_is_a_SLB() {
    ComponentDto project = componentDb.insertMainBranch();
    ComponentDto branch = componentDb.insertProjectBranch(project, b -> b.setKey("branch").setBranchType(BranchType.SHORT));
    logInAsProjectAdministrator(project);
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Not a long-living branch: 'branch'");

    ws.newRequest()
      .setParam("project", project.getKey())
      .setParam("branch", "branch")
      .execute();
  }

  @Test
  public void throw_NFE_if_no_project_permission() {
    ComponentDto project = componentDb.insertMainBranch();
    expectedException.expect(ForbiddenException.class);
    expectedException.expectMessage("Insufficient privileges");

    ws.newRequest()
      .setParam("project", project.getKey())
      .execute();
  }

  @Test
  public void show_global_setting() {
    tester.insert(new NewCodePeriodDto().setType(NewCodePeriodType.PREVIOUS_VERSION));

    ShowNewCodePeriodResponse response = ws.newRequest()
      .executeProtobuf(ShowNewCodePeriodResponse.class);

    assertResponse(response, "", "", Settings.NewCodePeriodType.PREVIOUS_VERSION, "", false);
  }

  @Test
  public void show_project_setting() {
    ComponentDto project = componentDb.insertMainBranch();
    logInAsProjectAdministrator(project);

    tester.insert(new NewCodePeriodDto()
      .setProjectUuid(project.uuid())
      .setType(NewCodePeriodType.NUMBER_OF_DAYS)
      .setValue("4"));

    ShowNewCodePeriodResponse response = ws.newRequest()
      .setParam("project", project.getKey())
      .executeProtobuf(ShowNewCodePeriodResponse.class);

    assertResponse(response, project.getKey(), "", Settings.NewCodePeriodType.NUMBER_OF_DAYS, "4", false);
  }

  @Test
  public void show_branch_setting() {
    ComponentDto project = componentDb.insertMainBranch();
    logInAsProjectAdministrator(project);

    ComponentDto branch = componentDb.insertProjectBranch(project, b -> b.setKey("branch"));

    tester.insert(new NewCodePeriodDto()
      .setProjectUuid(project.uuid())
      .setBranchUuid(branch.uuid())
      .setType(NewCodePeriodType.DATE)
      .setValue("2018-04-05"));

    ShowNewCodePeriodResponse response = ws.newRequest()
      .setParam("project", project.getKey())
      .setParam("branch", "branch")
      .executeProtobuf(ShowNewCodePeriodResponse.class);

    assertResponse(response, project.getKey(), "branch", Settings.NewCodePeriodType.DATE, "2018-04-05", false);
  }

  @Test
  public void show_inherited_project_setting() {
    ComponentDto project = componentDb.insertMainBranch();
    logInAsProjectAdministrator(project);
    tester.insert(new NewCodePeriodDto().setType(NewCodePeriodType.PREVIOUS_VERSION));

    ShowNewCodePeriodResponse response = ws.newRequest()
      .setParam("project", project.getKey())
      .executeProtobuf(ShowNewCodePeriodResponse.class);

    assertResponse(response, project.getKey(), "", Settings.NewCodePeriodType.PREVIOUS_VERSION, "", true);
  }

  @Test
  public void show_inherited_branch_setting_from_project() {
    ComponentDto project = componentDb.insertMainBranch();
    logInAsProjectAdministrator(project);

    ComponentDto branch = componentDb.insertProjectBranch(project, b -> b.setKey("branch"));

    tester.insert(new NewCodePeriodDto()
      .setProjectUuid(project.uuid())
      .setType(NewCodePeriodType.DATE)
      .setValue("2018-04-05"));

    ShowNewCodePeriodResponse response = ws.newRequest()
      .setParam("project", project.getKey())
      .setParam("branch", "branch")
      .executeProtobuf(ShowNewCodePeriodResponse.class);

    assertResponse(response, project.getKey(), "branch", Settings.NewCodePeriodType.DATE, "2018-04-05", true);
  }

  @Test
  public void show_inherited_branch_setting_from_global() {
    ComponentDto project = componentDb.insertMainBranch();
    logInAsProjectAdministrator(project);
    ComponentDto branch = componentDb.insertProjectBranch(project, b -> b.setKey("branch"));
    tester.insert(new NewCodePeriodDto().setType(NewCodePeriodType.NUMBER_OF_DAYS).setValue("3"));

    ShowNewCodePeriodResponse response = ws.newRequest()
      .setParam("project", project.getKey())
      .setParam("branch", "branch")
      .executeProtobuf(ShowNewCodePeriodResponse.class);

    assertResponse(response, project.getKey(), "branch", Settings.NewCodePeriodType.NUMBER_OF_DAYS, "3", true);
  }

  private void assertResponse(ShowNewCodePeriodResponse response, String projectKey, String branchKey, Settings.NewCodePeriodType type, String value, boolean inherited) {
    assertThat(response.getBranchKey()).isEqualTo(branchKey);
    assertThat(response.getProjectKey()).isEqualTo(projectKey);
    assertThat(response.getInherited()).isEqualTo(inherited);
    assertThat(response.getValue()).isEqualTo(value);
    assertThat(response.getType()).isEqualTo(type);
  }

  private void logInAsProjectAdministrator(ComponentDto project) {
    userSession.logIn().addProjectPermission(UserRole.ADMIN, project);
  }

}