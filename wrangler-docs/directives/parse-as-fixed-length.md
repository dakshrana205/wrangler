/*
 * Copyright © 2015-2016 Cask Data, Inc.
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

# Parse as Fixed Length

The PARSE-AS-FIXED-LENGTH directive parses a column as a fixed length record with widths
for each field specified.


## Syntax
```
parse-as-fixed-length <column> <width>[,<width>]* [<padding>]
```

## Usage Notes

Fixed-width text files are special cases of text files where the format is specified by
column widths, pad characters, and left or right alignment. Column widths are measured in
units of characters.

For example, if you have data in a text file where the first column always has exactly 10
characters, the second column has exactly 5, the third has exactly 12, and so on; this
would be categorized as a fixed-width text file.

If not defined, the `<padding>` character is assumed to be a space character.


## Example

Using this record as an example:
```
{
  "body": "12  10  ABCXYZ"
}
```

Applying this directive:
```
parse-as-fixed-length body 2,4,5,3
```

would result in this record:
```
{
  "body": "12  10  ABCXYZ",
  "body_1": "12",
  "body_2": "  10",
  "body_3": "  ABC",
  "body_4": "XYZ
}
```
