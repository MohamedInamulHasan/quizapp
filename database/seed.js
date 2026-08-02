const mongoose = require('mongoose');
const Question = require('../backend/models/Question');
require('dotenv').config({ path: '../backend/.env' });

const MONGODB_URI = process.env.MONGODB_URI || 'mongodb://127.0.0.1:27017/quizapp';

const sampleQuestions = [
  // General Science
  {
    question: "What is the chemical symbol for gold?",
    optionA: "Ag",
    optionB: "Au",
    optionC: "Gd",
    optionD: "Fe",
    correctAnswer: "B",
    category: "Science",
    difficulty: "easy"
  },
  {
    question: "Which planet is known as the Red Planet?",
    optionA: "Venus",
    optionB: "Mars",
    optionC: "Jupiter",
    optionD: "Saturn",
    correctAnswer: "B",
    category: "Science",
    difficulty: "easy"
  },
  {
    question: "What is the powerhouse of the cell?",
    optionA: "Nucleus",
    optionB: "Ribosome",
    optionC: "Mitochondria",
    optionD: "Lysosome",
    correctAnswer: "C",
    category: "Science",
    difficulty: "easy"
  },
  {
    question: "What is the speed of light in a vacuum?",
    optionA: "150,000 km/s",
    optionB: "300,000 km/s",
    optionC: "450,000 km/s",
    optionD: "600,000 km/s",
    correctAnswer: "B",
    category: "Science",
    difficulty: "medium"
  },
  {
    question: "Which element has the atomic number 1?",
    optionA: "Helium",
    optionB: "Hydrogen",
    optionC: "Lithium",
    optionD: "Oxygen",
    correctAnswer: "B",
    category: "Science",
    difficulty: "easy"
  },
  // Technology
  {
    question: "Who is known as the father of computer science?",
    optionA: "Bill Gates",
    optionB: "Steve Jobs",
    optionC: "Alan Turing",
    optionD: "Ada Lovelace",
    correctAnswer: "C",
    category: "Technology",
    difficulty: "easy"
  },
  {
    question: "What does HTTP stand for?",
    optionA: "HyperText Transfer Protocol",
    optionB: "HyperText Transmission Process",
    optionC: "High Transfer Text Protocol",
    optionD: "Hyperlink Transfer Text Protocol",
    correctAnswer: "A",
    category: "Technology",
    difficulty: "easy"
  },
  {
    question: "Which programming language is predominantly used for Android App development?",
    optionA: "Swift",
    optionB: "Kotlin",
    optionC: "C#",
    optionD: "Python",
    correctAnswer: "B",
    category: "Technology",
    difficulty: "easy"
  },
  {
    question: "What is the main database system used in the MERN stack?",
    optionA: "MySQL",
    optionB: "PostgreSQL",
    optionC: "MongoDB",
    optionD: "SQLite",
    correctAnswer: "C",
    category: "Technology",
    difficulty: "easy"
  },
  {
    question: "In what year was the first iPhone released?",
    optionA: "2005",
    optionB: "2007",
    optionC: "2008",
    optionD: "2010",
    correctAnswer: "B",
    category: "Technology",
    difficulty: "medium"
  },
  // Geography
  {
    question: "What is the capital of Australia?",
    optionA: "Sydney",
    optionB: "Canberra",
    optionC: "Melbourne",
    optionD: "Brisbane",
    correctAnswer: "B",
    category: "Geography",
    difficulty: "medium"
  },
  {
    question: "Which is the largest ocean on Earth?",
    optionA: "Atlantic Ocean",
    optionB: "Indian Ocean",
    optionC: "Pacific Ocean",
    optionD: "Arctic Ocean",
    correctAnswer: "C",
    category: "Geography",
    difficulty: "easy"
  },
  {
    question: "Which country is home to the Kangaroo?",
    optionA: "South Africa",
    optionB: "Australia",
    optionC: "New Zealand",
    optionD: "Austria",
    correctAnswer: "B",
    category: "Geography",
    difficulty: "easy"
  },
  {
    question: "What is the tallest mountain in the world?",
    optionA: "K2",
    optionB: "Mount Everest",
    optionC: "Mount Kilimanjaro",
    optionD: "Mount Fuji",
    correctAnswer: "B",
    category: "Geography",
    difficulty: "easy"
  },
  {
    question: "Which river is the longest in the world?",
    optionA: "Amazon River",
    optionB: "Nile River",
    optionC: "Yangtze River",
    optionD: "Mississippi River",
    correctAnswer: "B",
    category: "Geography",
    difficulty: "medium"
  },
  // History
  {
    question: "Who was the first president of the United States?",
    optionA: "Thomas Jefferson",
    optionB: "Abraham Lincoln",
    optionC: "George Washington",
    optionD: "John Adams",
    correctAnswer: "C",
    category: "History",
    difficulty: "easy"
  },
  {
    question: "In which year did World War II end?",
    optionA: "1918",
    optionB: "1939",
    optionC: "1945",
    optionD: "1950",
    correctAnswer: "C",
    category: "History",
    difficulty: "easy"
  },
  {
    question: "Who painted the Mona Lisa?",
    optionA: "Vincent van Gogh",
    optionB: "Pablo Picasso",
    optionC: "Leonardo da Vinci",
    optionD: "Michelangelo",
    correctAnswer: "C",
    category: "History",
    difficulty: "easy"
  },
  {
    question: "Which empire built the Colosseum in Rome?",
    optionA: "Greek Empire",
    optionB: "Roman Empire",
    optionC: "Byzantine Empire",
    optionD: "Ottoman Empire",
    correctAnswer: "B",
    category: "History",
    difficulty: "easy"
  },
  {
    question: "Who was the first human to journey into outer space?",
    optionA: "Neil Armstrong",
    optionB: "Yuri Gagarin",
    optionC: "Buzz Aldrin",
    optionD: "John Glenn",
    correctAnswer: "B",
    category: "History",
    difficulty: "medium"
  },
  // Hard/Mixed Questions
  {
    question: "How many bones are there in an adult human body?",
    optionA: "186",
    optionB: "206",
    optionC: "216",
    optionD: "226",
    correctAnswer: "B",
    category: "Science",
    difficulty: "medium"
  },
  {
    question: "Which of the following is NOT a programming paradigm?",
    optionA: "Object-Oriented",
    optionB: "Functional",
    optionC: "Logical",
    optionD: "Compilation",
    correctAnswer: "D",
    category: "Technology",
    difficulty: "medium"
  },
  {
    question: "Which country has the most natural lakes in the world?",
    optionA: "United States",
    optionB: "Canada",
    optionC: "Russia",
    optionD: "Brazil",
    correctAnswer: "B",
    category: "Geography",
    difficulty: "hard"
  },
  {
    question: "Who was the first emperor of China?",
    optionA: "Qin Shi Huang",
    optionB: "Han Wudi",
    optionC: "Tang Taizong",
    optionD: "Kublai Khan",
    correctAnswer: "A",
    category: "History",
    difficulty: "hard"
  },
  {
    question: "What is the capital of Switzerland?",
    optionA: "Zurich",
    optionB: "Bern",
    optionC: "Geneva",
    optionD: "Basel",
    correctAnswer: "B",
    category: "Geography",
    difficulty: "medium"
  }
];

const seedDB = async () => {
  try {
    await mongoose.connect(MONGODB_URI);
    console.log('Connected to Database for seeding.');
    
    // Clear existing questions
    await Question.deleteMany({});
    console.log('Cleared existing questions.');

    // Insert sample questions
    await Question.insertMany(sampleQuestions);
    console.log('Successfully seeded questions into the database.');
    
    mongoose.connection.close();
    console.log('Database connection closed.');
  } catch (error) {
    console.error('Error seeding database:', error);
    process.exit(1);
  }
};

seedDB();
