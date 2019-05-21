package jp.co.jcps.A01;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;

import jp.co.jcps.Bean.MessageBean;
import jp.co.jcps.Common.Validation;

/**
 * 学生ログイン画面のコントローラ
 */
@WebServlet("/Login")
public class LoginControllerServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	/**
	 * コンストラクタ
	 */
	public LoginControllerServlet() {
		super();
	}

	/**
	 * GETメソッドでリクエストされた場合の処理（初期表示時）
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// ログイン画面を呼び出し
		request.getRequestDispatcher("A01/Login.jsp").forward(request, response);

	}

	/**
	 * POSTメソッドでリクエストされた場合の処理
	 * 動的SQL生成のテスト
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// パラメータを初期化
		String loginNm = request.getParameter("loginNm");
		String password = request.getParameter("password");

		// messageBeanを初期化
		MessageBean messageBean = new MessageBean();

		// チェック処理
		Validation.checkAlphaNumeric(loginNm, "ログイン名", messageBean);
		Validation.checkAlphaNumeric(password, "パスワード", messageBean);
		Validation.checkLegalLengthString(loginNm, 30, "ログイン名", messageBean);
		Validation.checkLegalLengthString(password, 30, "パスワード", messageBean);
		Validation.checkRequired(loginNm, "ログイン名", messageBean);
		Validation.checkRequired(password, "パスワード", messageBean);

		if(messageBean.getMessageList().size() != 0) {
			// 入力値チェックでエラーがある場合
			request.setAttribute("messageBean", messageBean);
			// ログイン画面に遷移
			doGet(request, response);
		}

		// DB接続
		Connection con = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			// データソースの取得
			Context ctx = new InitialContext();
			DataSource ds = (DataSource) ctx.lookup("java:comp/env/jdbc/jc21ps");

			// データベースへ接続
			con = ds.getConnection();

			// SQLの実行
			String sql = "SELECT student_id FROM mst_student WHERE login_nm = '" +  loginNm + "'AND password = '" + password + "'";
			pstmt = con.prepareStatement(sql);
			rs = pstmt.executeQuery();

			// 行の最初に移動
			rs.first();

			// ログイン情報が取得できなかった場合はエラー
			if(rs.getRow() == 0) {
				messageBean.addMessageList("ログイン名もしくはパスワードが間違っています。");
				request.setAttribute("messageBean", messageBean);
				// ログイン画面に遷移
				doGet(request, response);
			}

			// Viewへ引き渡す値を設定
			if(rs.getString("student_id") != null) {
				// セッションを開始
				HttpSession session = request.getSession(true);
				// ログイン情報をセッションに保持
				session.setAttribute("studentId", rs.getString("student_id"));

				// ログイン成功の場合は履修講義一覧画面に遷移する
				request.getRequestDispatcher("/RegisteredLectureListControllerServlet").forward(request, response);
			}

		} catch (Exception e) {
			System.out.println(e.getMessage());
			request.getRequestDispatcher("ERROR/Error.jsp").forward(request, response);
		} finally {
			try {
				rs.close();
				pstmt.close();
				con.close();
			} catch (Exception e) {
				System.out.println(e.getMessage());
				request.getRequestDispatcher("ERROR/Error.jsp").forward(request, response);
			}
		}
	}
}
