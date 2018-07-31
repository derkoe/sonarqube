/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import * as React from 'react';
import * as theme from '../../../app/theme';
import Checkbox from '../../../components/controls/Checkbox';
import CheckIcon from '../../../components/icons-components/CheckIcon';
import { AlmRepository, IdentityProvider } from '../../../app/types';
import { getBaseUrl } from '../../../helpers/urls';
import { translate } from '../../../helpers/l10n';

interface Props {
  identityProvider: IdentityProvider;
  repository: AlmRepository;
  selected: boolean;
  toggleRepository: (repository: AlmRepository) => void;
}

export default class AlmRepositoryItem extends React.PureComponent<Props> {
  handleChange = () => {
    this.props.toggleRepository(this.props.repository);
  };

  render() {
    const { identityProvider, repository, selected } = this.props;
    const alreadyImported = Boolean(repository.linkedProjectKey);
    return (
      <Checkbox
        checked={selected || alreadyImported}
        disabled={alreadyImported}
        onCheck={this.handleChange}>
        <img
          alt={identityProvider.name}
          className="spacer-left"
          height={14}
          src={`${getBaseUrl()}/images/sonarcloud/${identityProvider.key}.svg`}
          style={{ opacity: alreadyImported ? 0.5 : 1 }}
          width={14}
        />
        <span className="spacer-left">{this.props.repository.label}</span>
        {alreadyImported && (
          <span className="big-spacer-left">
            <CheckIcon className="little-spacer-right" fill={theme.green} />
            {translate('onboarding.create_project.already_imported')}
          </span>
        )}
      </Checkbox>
    );
  }
}
