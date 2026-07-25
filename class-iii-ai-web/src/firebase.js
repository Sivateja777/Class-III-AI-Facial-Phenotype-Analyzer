import { initializeApp } from "firebase/app";
import { getAuth, GoogleAuthProvider } from "firebase/auth";
import { getFirestore } from "firebase/firestore";

const firebaseConfig = {
  apiKey: "AIzaSyAi1lYDAEFG0VqSSL5GdHArTOw7NIWw2r0",
  authDomain: "facial-phenotype-analyze-128d5.firebaseapp.com",
  databaseURL: "https://facial-phenotype-analyze-128d5-default-rtdb.firebaseio.com",
  projectId: "facial-phenotype-analyze-128d5",
  storageBucket: "facial-phenotype-analyze-128d5.firebasestorage.app",
  messagingSenderId: "89141093319",
  appId: "1:89141093319:web:a1b2c3d4e5f6g7h8i9j0k" // Fallback placeholder, API key is usually sufficient for simple Auth/Firestore
};

const app = initializeApp(firebaseConfig);
export const auth = getAuth(app);
export const db = getFirestore(app);
export const googleProvider = new GoogleAuthProvider();
