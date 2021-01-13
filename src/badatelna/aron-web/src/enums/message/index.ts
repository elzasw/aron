import { CZECH } from './czech';
import { ENGLISH } from './english';
import { Message } from './message';

const messages: Record<string, Record<Message, string>> = {
  CZECH,
  ENGLISH,
};

export { messages, Message };
