export type Status = 'pending' | 'processing' | 'done' | 'error';

export type SaveTo = 'original' | 'custom'

export type Item = {
  status: Status;
  file: string;
}
