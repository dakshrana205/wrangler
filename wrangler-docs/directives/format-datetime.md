<!--
Copyright © 2015-2016 Cask Data, Inc.

Licensed under the Apache License, Version 2.0 (the "License"); you may not
use this file except in compliance with the License. You may obtain a copy of
the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
License for the specific language governing permissions and limitations under
the License.
-->

# Format Datetime

The FORMAT-DATETIME directive formats CDAP datetime values to custom pattern strings.


## Syntax
```
format-datetime <datetime_column> "<pattern>"
```


## Usage Notes

The FORMAT-DATETIME directive will format CDAP datetime values to custom pattern strings.
Pattern is the format for the output string. 


If the column is `null` applying this directive is a no-op. 
The column to be formatted should be of type datetime.


## Examples
See [FORMAT-DATE](format-date.md) for an explanation and examples the pattern strings.
