# Using Book Writer

## Features

- **Simple Command**: Type `.write` to open the book writing interface
- **Title Support**: Add custom titles to your books
- **Automatic Splitting**: Text exceeding 100 pages automatically splits into multiple books with "Part 1", "Part 2" suffixes
- **Book Calculation**: Calculate how many books you'll need, and about how long writing will take
- **Adjustable Speed**: Choose the delay between books with a slider (1 to 10 seconds, 3 by default)
- **Action Bar Feedback**: Real-time progress updates via action bar messages
- **Async Writing**: Non-blocking background writing to prevent game freezes
- **Inventory Management**: Automatic detection of available books

## Usage

1. Open your inventory with `WRITABLE_BOOK` items
2. Type `.write` in chat
3. Enter a title and paste your text
4. Click "Write to Book" or "Calculate" to check requirements
5. Books will be signed and written to your inventory with the delay you chose between each (3 seconds by default)

## Technical Details

- **Page Limit**: 256 characters per page
- **Book Limit**: 100 pages per book
- **Write Delay**: 1 to 10 seconds between books, saved in `config/book-writer.json`
- **Supported Version**: Minecraft 26.2
- **Architecture**: Pure client-side, event-driven
