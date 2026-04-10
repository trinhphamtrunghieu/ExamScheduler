import { useState } from "react";
import { API_BASE } from "./common.tsx";
import NavBar from "./NavBar.tsx";

function AddCourse() {
  const [course, setCourse] = useState({
    maMonHoc: "",
    tenMonHoc: "",
    ten_gv_dung_lop: "",
    ngay_bat_dau: "",
    ngay_ket_thuc: "",
    thoi_luong_thi: "",
  });

  const handleSubmit = (e) => {
    e.preventDefault();
    const payload = {
      id: course.maMonHoc,
      name: course.tenMonHoc,
      teacher: course.ten_gv_dung_lop,
      startDate: course.ngay_bat_dau || null,
      endDate: course.ngay_ket_thuc || null,
      duration: course.thoi_luong_thi ? Number(course.thoi_luong_thi) : null,
    };
    fetch(`${API_BASE}/subjects/add`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload),
    })
      .then((res) => res.json())
      .then((data) => alert("Course added successfully!"))
      .catch((error) => console.error("Error adding course:", error));
  };

  return (
    <div id="root">
      <NavBar />
      <div className="spacing"></div> {/* Spacing */}
      <div className="content-area">
        <h2 className="text-3xl font-semibold text-indigo-600 mb-6">Add Course</h2>

        <form onSubmit={handleSubmit} className="w-full max-w-md space-y-4">
          <div className="flex items-center mb-4">
            <label htmlFor="maMonHoc" className="mr-2 text-lg font-semibold">
              Mã Môn Học:
            </label>
            <input
              type="text"
              id="maMonHoc"
              placeholder="Mã Môn Học"
              required
              value={course.maMonHoc}
              onChange={(e) =>
                setCourse({ ...course, maMonHoc: e.target.value })
              }
              className="p-2 border border-gray-300 rounded-lg shadow-sm focus:outline-none focus:ring-2 focus:ring-indigo-600"
            />
          </div>

          <div className="flex items-center mb-4">
            <label htmlFor="tenMonHoc" className="mr-2 text-lg font-semibold">
              Tên Môn Học:
            </label>
            <input
              type="text"
              id="tenMonHoc"
              placeholder="Tên Môn Học"
              required
              value={course.tenMonHoc}
              onChange={(e) =>
                setCourse({ ...course, tenMonHoc: e.target.value })
              }
              className="p-2 border border-gray-300 rounded-lg shadow-sm focus:outline-none focus:ring-2 focus:ring-indigo-600"
            />
          </div>

          <div className="flex items-center mb-4">
            <label htmlFor="ten_gv_dung_lop" className="mr-2 text-lg font-semibold">
              Tên Giảng Viên:
            </label>
            <input
              type="text"
              id="ten_gv_dung_lop"
              placeholder="Tên Giảng Viên"
              value={course.ten_gv_dung_lop}
              onChange={(e) =>
                setCourse({ ...course, ten_gv_dung_lop: e.target.value })
              }
              className="p-2 border border-gray-300 rounded-lg shadow-sm focus:outline-none focus:ring-2 focus:ring-indigo-600"
            />
          </div>

          <div className="flex items-center mb-4">
            <label htmlFor="ngay_bat_dau" className="mr-2 text-lg font-semibold">
              Ngày Bắt Đầu:
            </label>
            <input
              type="date"
              id="ngay_bat_dau"
              value={course.ngay_bat_dau}
              onChange={(e) =>
                setCourse({ ...course, ngay_bat_dau: e.target.value })
              }
              className="p-2 border border-gray-300 rounded-lg shadow-sm focus:outline-none focus:ring-2 focus:ring-indigo-600"
            />
          </div>

          <div className="flex items-center mb-4">
            <label htmlFor="ngay_ket_thuc" className="mr-2 text-lg font-semibold">
              Ngày Kết Thúc:
            </label>
            <input
              type="date"
              id="ngay_ket_thuc"
              value={course.ngay_ket_thuc}
              onChange={(e) =>
                setCourse({ ...course, ngay_ket_thuc: e.target.value })
              }
              className="p-2 border border-gray-300 rounded-lg shadow-sm focus:outline-none focus:ring-2 focus:ring-indigo-600"
            />
          </div>

          <div className="flex items-center mb-4">
            <label htmlFor="thoi_luong_thi" className="mr-2 text-lg font-semibold">
              Thời Lượng Thi (phút):
            </label>
            <input
              type="number"
              id="thoi_luong_thi"
              placeholder="Thời Lượng Thi (phút)"
              value={course.thoi_luong_thi}
              onChange={(e) =>
                setCourse({ ...course, thoi_luong_thi: e.target.value })
              }
              className="p-2 border border-gray-300 rounded-lg shadow-sm focus:outline-none focus:ring-2 focus:ring-indigo-600"
            />
          </div>

          <div className="flex justify-center mt-4">
            <button
              type="submit"
              className="px-6 py-2 text-white bg-indigo-600 rounded-lg hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-indigo-600"
            >
              Add Course
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

export default AddCourse;
