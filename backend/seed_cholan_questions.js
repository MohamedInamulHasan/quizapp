const dns = require('dns');
dns.setServers(['8.8.8.8', '8.8.4.4']);

const mongoose = require('mongoose');
const Question = require('./models/Question');
require('dotenv').config();

const MONGODB_URI = process.env.MONGODB_URI || 'mongodb://127.0.0.1:27017/quizapp';

const cholanQuestions = [
  {
    question: "Who was Rajaraja Cholan?",
    optionA: "A famous poet",
    optionB: "One of the greatest rulers of the Chola Empire",
    optionC: "A merchant from South India",
    optionD: "A temple architect",
    correctAnswer: "B",
    category: "Passage Study",
    difficulty: "medium"
  },
  {
    question: "During which years did Rajaraja Cholan rule the Chola Empire?",
    optionA: "950 CE – 980 CE",
    optionB: "1014 CE – 1040 CE",
    optionC: "985 CE – 1014 CE",
    optionD: "900 CE – 930 CE",
    correctAnswer: "C",
    category: "Passage Study",
    difficulty: "medium"
  },
  {
    question: "What helped Rajaraja Cholan become one of South India's greatest rulers?",
    optionA: "His poetry and music",
    optionB: "His military leadership, administrative reforms, and cultural achievements",
    optionC: "His farming skills",
    optionD: "His trading business",
    correctAnswer: "B",
    category: "Passage Study",
    difficulty: "medium"
  },
  {
    question: "Which region was conquered during Rajaraja Cholan's reign?",
    optionA: "Japan",
    optionB: "Australia",
    optionC: "Sri Lanka",
    optionD: "Egypt",
    correctAnswer: "C",
    category: "Passage Study",
    difficulty: "medium"
  },
  {
    question: "What made the Chola Empire a dominant maritime power?",
    optionA: "Strong cavalry",
    optionB: "Powerful navy",
    optionC: "Large air force",
    optionD: "Mountain forts",
    correctAnswer: "B",
    category: "Passage Study",
    difficulty: "medium"
  },
  {
    question: "How did Rajaraja Cholan improve administration?",
    optionA: "By dividing the empire into provinces, districts, and villages",
    optionB: "By closing village councils",
    optionC: "By reducing trade",
    optionD: "By removing taxes completely",
    correctAnswer: "A",
    category: "Passage Study",
    difficulty: "medium"
  },
  {
    question: "What did Rajaraja Cholan encourage through his administrative system?",
    optionA: "Foreign rule",
    optionB: "Local self-governance",
    optionC: "No village administration",
    optionD: "Private kingdoms",
    correctAnswer: "B",
    category: "Passage Study",
    difficulty: "medium"
  },
  {
    question: "Which sector contributed to the prosperity of the Chola Empire?",
    optionA: "Agriculture",
    optionB: "Space research",
    optionC: "Oil production",
    optionD: "Automobile manufacturing",
    correctAnswer: "A",
    category: "Passage Study",
    difficulty: "medium"
  },
  {
    question: "The Chola Empire traded with which of the following regions?",
    optionA: "Antarctica",
    optionB: "Southeast Asia, China, and the Arab world",
    optionC: "North Pole",
    optionD: "South America only",
    correctAnswer: "B",
    category: "Passage Study",
    difficulty: "medium"
  },
  {
    question: "Rajaraja Cholan was a patron of which fields?",
    optionA: "Art, architecture, literature, and religion",
    optionB: "Cricket and football",
    optionC: "Space science only",
    optionD: "Film industry",
    correctAnswer: "A",
    category: "Passage Study",
    difficulty: "medium"
  },
  {
    question: "What is the name of the famous temple built by Rajaraja Cholan?",
    optionA: "Meenakshi Temple",
    optionB: "Brihadeeswarar Temple",
    optionC: "Konark Sun Temple",
    optionD: "Golden Temple",
    correctAnswer: "B",
    category: "Passage Study",
    difficulty: "medium"
  },
  {
    question: "In which year was the Brihadeeswarar Temple completed?",
    optionA: "950 CE",
    optionB: "1010 CE",
    optionC: "1100 CE",
    optionD: "1200 CE",
    correctAnswer: "B",
    category: "Passage Study",
    difficulty: "medium"
  },
  {
    question: "Why is the Brihadeeswarar Temple famous?",
    optionA: "It is made of wood",
    optionB: "It is an example of remarkable Dravidian architecture and engineering",
    optionC: "It floats on water",
    optionD: "It is underground",
    correctAnswer: "B",
    category: "Passage Study",
    difficulty: "medium"
  },
  {
    question: "Why are Rajaraja Cholan's inscriptions important?",
    optionA: "They contain movie scripts",
    optionB: "They provide valuable historical information about the Chola period",
    optionC: "They describe modern technology",
    optionD: "They are written in English",
    correctAnswer: "B",
    category: "Passage Study",
    difficulty: "medium"
  },
  {
    question: "Why is Rajaraja Cholan remembered today?",
    optionA: "He was a famous singer",
    optionB: "He was one of India's greatest emperors and strengthened the Chola Empire",
    optionC: "He invented the wheel",
    optionD: "He discovered electricity",
    correctAnswer: "B",
    category: "Passage Study",
    difficulty: "medium"
  }
];

mongoose.connect(MONGODB_URI).then(async () => {
  console.log('Connected to MongoDB.');
  await Question.deleteMany({});
  const inserted = await Question.insertMany(cholanQuestions);
  console.log(`🎉 Successfully seeded ${inserted.length} Rajaraja Cholan questions into MongoDB!`);
  mongoose.disconnect();
}).catch(err => {
  console.error('Error:', err);
});
