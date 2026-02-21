#!/usr/bin/env python3
import json
import os
import shutil
import subprocess
import sys
import tempfile

import pytest

# 테스트 대상 모듈 import
sys.path.insert(0, os.path.dirname(__file__))
import layer_doc_reminder as sut

SCRIPT_PATH = os.path.join(os.path.dirname(__file__), "layer_doc_reminder.py")


@pytest.fixture(autouse=True)
def isolated_reminder_dir(tmp_path, monkeypatch):
    """각 테스트마다 격리된 임시 디렉토리 사용"""
    reminder_dir = str(tmp_path / "reminders")
    monkeypatch.setattr(sut, "REMINDER_DIR", reminder_dir)
    yield reminder_dir


# ── detect_layer ──


class TestDetectLayer:
    def test_domain(self):
        assert sut.detect_layer("/project/task/domain/model/Task.kt") == "domain"

    def test_application(self):
        assert sut.detect_layer("/project/task/application/service/TaskService.kt") == "application"

    def test_infrastructure(self):
        assert sut.detect_layer("/project/task/infrastructure/persistence/TaskTable.kt") == "infrastructure"

    def test_presentation(self):
        assert sut.detect_layer("/project/task/presentation/controller/TaskController.kt") == "presentation"

    def test_unrelated_path(self):
        assert sut.detect_layer("/project/build.gradle.kts") is None

    def test_common_config(self):
        assert sut.detect_layer("/project/common/config/SecurityConfig.kt") is None


# ── is_security_related ──


class TestIsSecurityRelated:
    def test_security_config(self):
        assert sut.is_security_related("/project/common/config/SecurityConfig.kt") is True

    def test_security_case_insensitive(self):
        assert sut.is_security_related("/project/common/config/SECURITY_CONFIG.kt") is True

    def test_non_security_config(self):
        assert sut.is_security_related("/project/common/config/WebMvcConfig.kt") is False

    def test_security_outside_common_config(self):
        assert sut.is_security_related("/project/auth/domain/model/SecurityToken.kt") is False


# ── should_remind / mark_reminded ──


class TestReminderMarker:
    def test_should_remind_when_no_marker(self):
        assert sut.should_remind("session-1", "domain") is True

    def test_should_not_remind_after_marked(self):
        sut.mark_reminded("session-1", "domain")
        assert sut.should_remind("session-1", "domain") is False

    def test_different_session_should_remind(self):
        sut.mark_reminded("session-1", "domain")
        assert sut.should_remind("session-2", "domain") is True

    def test_different_layer_should_remind(self):
        sut.mark_reminded("session-1", "domain")
        assert sut.should_remind("session-1", "infrastructure") is True


# ── main (subprocess 통합 테스트) ──


def run_hook(input_data, reminder_dir):
    """hook 스크립트를 subprocess로 실행"""
    env = os.environ.copy()
    result = subprocess.run(
        [sys.executable, SCRIPT_PATH],
        input=json.dumps(input_data),
        capture_output=True,
        text=True,
        env=env,
    )
    return result


class TestMain:
    @pytest.fixture(autouse=True)
    def setup_reminder_dir(self, isolated_reminder_dir, monkeypatch):
        """subprocess에서도 격리된 디렉토리를 쓰도록 환경변수로 전달은 불가하므로,
        실제 REMINDER_DIR을 임시로 교체"""
        self.reminder_dir = isolated_reminder_dir
        # subprocess는 별도 프로세스이므로 monkeypatch가 안 먹힘.
        # 대신 실제 /tmp 경로를 사용하되, 고유 session_id로 격리.
        self.session_id = f"test-{os.getpid()}-{id(self)}"

    def _make_input(self, file_path, session_id=None):
        return {
            "session_id": session_id or self.session_id,
            "hook_event_name": "PreToolUse",
            "tool_name": "Edit",
            "tool_input": {"file_path": file_path},
        }

    def test_first_domain_edit_blocks(self):
        result = run_hook(
            self._make_input("/project/task/domain/model/Task.kt"),
            self.reminder_dir,
        )
        assert result.returncode == 2
        assert "domain" in result.stderr
        assert "docs/layers/domain.md" in result.stderr

    def test_second_domain_edit_passes(self):
        # 첫 번째: 차단
        run_hook(
            self._make_input("/project/task/domain/model/Task.kt"),
            self.reminder_dir,
        )
        # 두 번째: 통과
        result = run_hook(
            self._make_input("/project/task/domain/model/Task.kt"),
            self.reminder_dir,
        )
        assert result.returncode == 0

    def test_docs_path_always_passes(self):
        result = run_hook(
            self._make_input("/project/docs/layers/domain.md"),
            self.reminder_dir,
        )
        assert result.returncode == 0

    def test_test_file_suffix_always_passes(self):
        result = run_hook(
            self._make_input("/project/task/domain/model/TaskTest.kt"),
            self.reminder_dir,
        )
        assert result.returncode == 0

    def test_src_test_directory_always_passes(self):
        result = run_hook(
            self._make_input("/project/src/test/kotlin/task/domain/model/Task.kt"),
            self.reminder_dir,
        )
        assert result.returncode == 0

    def test_bc_named_test_triggers_reminder(self):
        """BC 이름이 test여도 src/test/가 아니므로 차단되어야 함"""
        result = run_hook(
            self._make_input("/project/test/domain/model/Task.kt"),
            self.reminder_dir,
        )
        assert result.returncode == 2
        assert "domain" in result.stderr

    def test_unrelated_file_passes(self):
        result = run_hook(
            self._make_input("/project/build.gradle.kts"),
            self.reminder_dir,
        )
        assert result.returncode == 0

    def test_security_config_blocks(self):
        result = run_hook(
            self._make_input("/project/common/config/SecurityConfig.kt"),
            self.reminder_dir,
        )
        assert result.returncode == 2
        assert "security" in result.stderr.lower()
        assert "spring-security-7.md" in result.stderr

    def test_different_session_blocks_again(self):
        # 세션 1: 차단
        run_hook(
            self._make_input("/project/task/domain/model/Task.kt", session_id="session-A"),
            self.reminder_dir,
        )
        # 세션 2: 다시 차단
        result = run_hook(
            self._make_input("/project/task/domain/model/Task.kt", session_id="session-B"),
            self.reminder_dir,
        )
        assert result.returncode == 2

    def test_empty_file_path_passes(self):
        result = run_hook(
            {"session_id": self.session_id, "tool_input": {"file_path": ""}},
            self.reminder_dir,
        )
        assert result.returncode == 0

    def test_invalid_json_passes(self):
        """잘못된 JSON 입력 시 허용 (안전 기본값)"""
        result = subprocess.run(
            [sys.executable, SCRIPT_PATH],
            input="not json",
            capture_output=True,
            text=True,
        )
        assert result.returncode == 0

    def teardown_method(self):
        """subprocess 테스트에서 생성된 마커 정리"""
        import glob
        for f in glob.glob(f"/tmp/claude_layer_reminders/{self.session_id}_*"):
            os.remove(f)
        for f in glob.glob("/tmp/claude_layer_reminders/session-A_*"):
            os.remove(f)
        for f in glob.glob("/tmp/claude_layer_reminders/session-B_*"):
            os.remove(f)
