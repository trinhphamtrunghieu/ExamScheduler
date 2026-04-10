import { BrowserRouter as Router, Routes, Route } from "react-router-dom";
import "./App.css";
import AddCourse from "./AddCourse"
import AddStudent from "./AddStudent"
import RegisterStudent from "./RegisterStudent"
import NavBar from "./NavBar"
import Students from "./Student"
import Subjects from "./Subject"
import Registrations from "./Registration"
import Generate from "./Generate"
import Configuration from "./Configuration"

function Home() {
  return (
    <div id="root">
      <NavBar />
      <div className="spacing" > </div>
      <div className="content-area"> </div>
    </div>
  );
}

export default function App() {
  return (
    <Router>
      <Routes>
        <Route path="/config" element={<Configuration />} />
        <Route path="/" element={<Home />} />
        <Route path="/students" element={<Students />} />
        <Route path="/subjects" element={<Subjects />} />
        <Route path="/registrations" element={<Registrations />} />
        <Route path="/generate" element={<Generate />} />
        <Route path="/add-course" element={<AddCourse />} />
        <Route path="/add-student" element={<AddStudent />} />
        <Route path="/register" element={<RegisterStudent />} />
      </Routes>
    </Router>
  );
}