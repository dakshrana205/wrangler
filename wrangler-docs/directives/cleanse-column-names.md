/*
 * Copyright © 2017-2019 Cask Data, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

# Cleanse Column Names

The CLEANSE-COLUMN-NAMES directive sanatizes column names, following these rules:

* Trim leading and trailing spaces
* Lowercases the column name
* Replaces any character that are not one of `[A-Z][a-z][0-9]` or `_` with an underscore (`_`)


## Syntax
```
cleanse-column-names
```


## Example

Using this record as an example:
```
{
  "COL1": 1,
  "col:2": 2,
  "Col3": 3,
  "COLUMN4": 4,
  "col!5": 5
}
```

Applying this directive:
```
cleanse-column-names
```

would result in this record:
```
{
  "col1": 1,
  "col_2": 2,
  "col3": 3,
  "column4": 4,
  "col_5": 5
}
```
