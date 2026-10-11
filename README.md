# Data Visualization System

**English** | [Türkçe](README.tr.md)

A desktop data visualization app written in Java and JavaFX: load a CSV file or paste a table, let the app detect the column types, and build a dashboard of charts with grouping, aggregation and filters.

> Work in progress.

## Planned Features (MVP)

- CSV / TSV import with automatic delimiter, encoding, header and column type detection (numbers, dates, text; Turkish number formats such as `1.234,56`)
- Pasting a table copied from Excel or a web page
- Data preview table and column summaries (min, max, average, empty cells)
- Chart builder: bar, stacked bar, line, area, pie, scatter and histogram
- Grouping and aggregation (sum, average, count, min, max), date grouping by day / month / year, filters
- Dashboard with several chart cards, saved to and opened from a project file
- Export charts as PNG
- Built-in sample datasets from open data sources, with source and license shown in the app
- Turkish and English, light and dark theme, Windows installer

## Future Plans

- Excel (.xlsx) import
- Live data sources (APIs, databases)
- Dashboard export as PDF
