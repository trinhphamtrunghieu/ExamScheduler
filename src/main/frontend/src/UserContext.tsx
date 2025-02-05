import { createContext, useState, useContext, ReactNode } from "react";

// Define types for the context
interface UserContextType {
  userRole: string | null;
  studentId: string;
  login: (role: string, studentId?: string) => void;
  logout: () => void;
}

// Create the context with a default value of undefined
const UserContext = createContext<UserContextType | undefined>(undefined);

interface UserProviderProps {
  children: ReactNode;
}

export function UserProvider({ children }: UserProviderProps) {
  const [userRole, setUserRole] = useState<string | null>(localStorage.getItem("userRole") || null);
  const [studentId, setStudentId] = useState(localStorage.getItem("studentId") || "");

  // Function to log in
  const login = (role: string, studentId = "") => {
    setUserRole(role);
    setStudentId(studentId);
    localStorage.setItem("userRole", role);
    if (studentId) {
      localStorage.setItem("studentId", studentId);
    }
  };

  // Function to log out
  const logout = () => {
    setUserRole(null);
    setStudentId("");
    localStorage.removeItem("userRole");
    localStorage.removeItem("studentId");
  };

  return (
    <UserContext.Provider value={{ userRole, studentId, login, logout }}>
      {children}
    </UserContext.Provider>
  );
}

export function useUser() {
  const context = useContext(UserContext);
  if (!context) {
    throw new Error("useUser must be used within a UserProvider");
  }
  return context;
}
