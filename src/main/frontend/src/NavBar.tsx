import { Link } from "react-router-dom";

function Navbar() {
  return (
    <div className="navbar p-4 bg-gray-200 shadow-md">
      <h2 className="text-3xl font-semibold mb-6">Navigation</h2>
      <nav>
        <Link to="/" className="block px-4 py-2 rounded-md hover:bg-indigo-700">
          Home
        </Link>

        <ul className="space-y-4">
          <li>
            <Link to="/students" className="block px-4 py-2 rounded-md hover:bg-indigo-700">
              Danh sách sinh viên
            </Link>
          </li>
          <li>
            <Link to="/subjects" className="block px-4 py-2 rounded-md hover:bg-indigo-700">
              Danh sách môn học
            </Link>
          </li>
          <li>
            <Link to="/registrations" className="block px-4 py-2 rounded-md hover:bg-indigo-700">
              Danh sách đăng ký
            </Link>
          </li>
          <li>
            <Link to="/generate" className="block px-4 py-2 rounded-md hover:bg-indigo-700">
              Tạo lịch thi
            </Link>
          </li>
          <li>
            <Link to="/add-course" className="block px-4 py-2 rounded-md hover:bg-indigo-700">
              Tạo lớp học
            </Link>
          </li>
          <li>
            <Link to="/add-student" className="block px-4 py-2 rounded-md hover:bg-indigo-700">
              Tạo sinh viên
            </Link>
          </li>
          <li>
            <Link to="/register" className="block px-4 py-2 rounded-md hover:bg-indigo-700">
              Đăng ký thi
            </Link>
          </li>
          <li>
            <Link to="/config" className="block px-4 py-2 rounded-md hover:bg-indigo-700">
              Cài đặt
            </Link>
          </li>
        </ul>
      </nav>
    </div>
  );
}

export default Navbar;
