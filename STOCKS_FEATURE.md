# Stocks Widget Feature

## Overview
The stocks widget feature allows users to view real-time stock information directly in the HomeScreen. Users can select up to 5 stocks to display in a compact widget format.

## Features

### Stocks Widget
- Displays up to 5 selected stocks in a horizontal scrollable layout
- Shows stock symbol, current price, price change, and percentage change
- Color-coded indicators (green for positive, red for negative changes)
- Loading state while fetching data
- Tap to customize selection

### Stock Selection Dialog
- Search functionality to find stocks by symbol or company name
- Popular stocks suggestions
- Add/remove stocks from selection (max 5)
- Real-time search with debouncing
- Persistent storage of selected stocks

## Implementation Details

### Data Models
- `Stock`: Main data model for stock information
- `StockQuote`: API response model for stock quotes
- `StockSearchResult`: Search result model

### Components
- `StocksWidget`: Main widget component displaying selected stocks
- `StockCard`: Individual stock card component
- `StockSelectionDialog`: Dialog for selecting stocks
- `StockSearchItem`: Search result item component
- `SelectedStockChip`: Selected stock chip component

### Repository
- `StocksRepository`: Handles API calls and local storage
- Mock data implementation for demonstration
- SharedPreferences for persistent storage

### API Integration
- Uses [Alpha Vantage API](https://www.alphavantage.co/) for stock data (NASDAQ-licensed provider)
- Retrofit for HTTP requests
- OkHttp for networking
- Real-time stock quotes and search functionality
- Rate limiting implemented (5 calls per minute for free tier)

## Usage

1. Navigate to the "Widgets" tab in the HomeScreen
2. Tap on the "Stocks" widget
3. Use the search bar to find stocks
4. Select up to 5 stocks to display
5. Tap "Save Selection" to update the widget

## API Configuration

The app is already configured to use Alpha Vantage API with the provided API key. The implementation includes:

- Real-time stock quotes using the GLOBAL_QUOTE endpoint
- Stock search using the SYMBOL_SEARCH endpoint
- Rate limiting to respect Alpha Vantage's free tier limits (5 calls per minute)
- Fallback to mock data if API calls fail
- Individual stock quote calls (Alpha Vantage doesn't support batch quotes in free tier)

### Rate Limits
- Free tier: 5 API calls per minute
- The app implements a 1.2-second delay between calls to stay within limits
- For higher usage, consider upgrading to Alpha Vantage's premium plans

## Dependencies Added
- Retrofit 2.11.0
- OkHttp 4.12.0
- Gson (already included)

## Permissions
- Internet permission (already present in AndroidManifest.xml) 