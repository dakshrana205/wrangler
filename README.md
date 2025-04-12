# CDAP Wrangler

Wrangler is a powerful data preparation and transformation tool that provides a simple and intuitive interface for data wrangling. It allows users to clean, transform, and prepare data for analysis through a series of directives.

## Features

- **Data Transformation**: Clean, transform, and prepare data using a wide range of directives
- **Multiple Data Sources**: Support for various data sources including files, databases, and cloud storage
- **Data Quality**: Built-in data quality checks and validation
- **Extensible**: Custom directives can be added to extend functionality
- **REST API**: Programmatic access through REST endpoints
- **Schema Management**: Automatic schema inference and management

## Prerequisites

- Java 8 or higher
- Maven 3.5.0 or higher
- Git

## Building the Project

1. Clone the repository:
```bash
git clone https://github.com/cdapio/wrangler.git
cd wrangler
```

2. Build the project:
```bash
mvn clean install
```

## Project Structure

- `wrangler-api`: Core API definitions
- `wrangler-proto`: Protocol buffer definitions
- `wrangler-test`: Testing framework
- `wrangler-core`: Core implementation
- `wrangler-storage`: Storage implementations
- `wrangler-service`: REST service implementation
- `wrangler-transform`: Transformation implementations

## Available Directives

Wrangler provides a rich set of directives for data transformation:

### Column Operations
- `set-header`: Set column headers
- `keep`: Keep specific columns
- `drop`: Drop specific columns
- `rename`: Rename columns
- `merge`: Merge columns

### Data Transformation
- `parse-as-csv`: Parse CSV data
- `parse-as-json`: Parse JSON data
- `parse-as-xml`: Parse XML data
- `parse-as-avro`: Parse Avro data
- `parse-as-log`: Parse log data
- `parse-as-excel`: Parse Excel data

### Data Cleaning
- `cleanse-column-names`: Clean column names
- `fill-null-or-empty`: Fill null or empty values
- `trim`: Trim whitespace
- `lower`: Convert to lowercase
- `upper`: Convert to uppercase
- `title-case`: Convert to title case

### Data Validation
- `validate-standard`: Validate data against standard formats
- `validate-regex`: Validate using regular expressions
- `validate-email`: Validate email addresses
- `validate-phone`: Validate phone numbers

### Date and Time
- `parse-date`: Parse date strings
- `parse-timestamp`: Parse timestamp strings
- `format-date`: Format dates
- `format-timestamp`: Format timestamps

### Text Processing
- `extract-regex-groups`: Extract regex groups
- `find-and-replace`: Find and replace text
- `split-to-rows`: Split text to rows
- `split-to-columns`: Split text to columns

## Usage Examples

### Basic Data Transformation
```wrangler
parse-as-csv :body ',' false
set-header :a,b,c
drop :a
rename :b:new_b
```

### Data Cleaning
```wrangler
parse-as-csv :body ',' false
cleanse-column-names
fill-null-or-empty :col1 'default'
trim :col2
lower :col3
```

### Date Processing
```wrangler
parse-date :date_col 'yyyy-MM-dd'
format-date :date_col 'MM/dd/yyyy'
```

## REST API

Wrangler provides a REST API for programmatic access:

### Endpoints
- `POST /v2/wrangler/execute`: Execute wrangler directives
- `GET /v2/wrangler/directives`: List available directives
- `GET /v2/wrangler/directives/{name}`: Get directive details

## Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

## Support

For support, please open an issue in the GitHub repository or contact the CDAP team.

## Acknowledgments

- CDAP Team
- Contributors
- Open Source Community
