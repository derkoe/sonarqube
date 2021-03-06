/*
 * SonarQube
 * Copyright (C) 2009-2021 SonarSource SA
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
import Dropdown from 'sonar-ui-common/components/controls/Dropdown';
import { PopupPlacement } from 'sonar-ui-common/components/ui/popups';
import { translateWithParameters } from 'sonar-ui-common/helpers/l10n';
import LineOptionsPopup from './LineOptionsPopup';

export interface LineNumberProps {
  line: T.SourceLine;
}

export function LineNumber({ line }: LineNumberProps) {
  const { line: lineNumber } = line;
  const hasLineNumber = !!lineNumber;
  return hasLineNumber ? (
    <td className="source-meta source-line-number" data-line-number={lineNumber}>
      <Dropdown
        overlay={<LineOptionsPopup line={line} />}
        overlayPlacement={PopupPlacement.RightTop}>
        <span
          aria-label={translateWithParameters('source_viewer.line_X', lineNumber)}
          role="button">
          {lineNumber}
        </span>
      </Dropdown>
    </td>
  ) : (
    <td className="source-meta source-line-number" />
  );
}

export default React.memo(LineNumber);
