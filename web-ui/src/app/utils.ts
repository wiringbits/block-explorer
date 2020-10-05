export function getNumberOfRowsForScreen(height: number): number {
    if (height < 550) {
        return 10;
    }
    return Math.min(10 + Math.ceil((height - 550) / 20), 100);
}

export function truncate(fullStr, frontChars, backChars, separator = null) {
    if (fullStr.length <= frontChars + backChars) {
        return fullStr;
    }

    separator = separator || ' ... ';
    return fullStr.substr(0, frontChars) +
        separator +
        fullStr.substr(fullStr.length - backChars);
}

export function amAgo(timestamp: number): string {
    const current = new Date().getTime() / 1000;
    let diff = current - timestamp;

    const units = [
      {unit: 's', interval: 60},
      {unit: 'm', interval: 60},
      {unit: 'h', interval: 24},
      {unit: 'd', interval: -1}
    ];

    let i = 0;
    while (diff > 0 && i < units.length) {
      if (units[i].interval > 0 && diff / units[i].interval >= 1) {
        diff /= units[i].interval;
        i++;
      } else {
        break;
      }
    }

    return diff.toFixed(0) + units[i].unit + ' ago';
}