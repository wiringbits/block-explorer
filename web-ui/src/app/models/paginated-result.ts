
export class PaginatedResult<T> {
  offset: number;
  limit: number;
  total: number;
  data: T[];
}
