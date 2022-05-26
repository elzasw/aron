import { CZECH } from './czech';
import { ENGLISH } from './english';
import { GERMAN } from './german';
import { FRENCH } from './french';
import { Message } from './message';

const messages: Record<string, Record<Message, string>> = {
  CZECH,
  ENGLISH,
  GERMAN,
  FRENCH,
};

export { messages, Message };
