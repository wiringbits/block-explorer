export function getNumberOfRowsForScreen(height: number): number {
    if (height < 550) {
        return 10;
    }
    return Math.min(10 + Math.ceil((height - 550) / 20), 100);
}
