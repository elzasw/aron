import { CZECH } from './czech';
import { ENGLISH } from './english';
import { GERMAN } from './german';
import { Message } from './message';

const messages: Record<string, Record<Message, string>> = {
  CZECH,
  ENGLISH,
  GERMAN,
};

export { messages, Message };
