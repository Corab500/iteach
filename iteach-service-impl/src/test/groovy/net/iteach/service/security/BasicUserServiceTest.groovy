package net.iteach.service.security

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import net.iteach.api.SchoolService;
import net.iteach.test.AbstractIntegrationTest;

class BasicUserServiceTest extends AbstractIntegrationTest {
	
	@Autowired
	private BasicUserService service
	
	@Test
	void admin() {
		def details = service.loadUserByUsername("admin")
		assert details != null
		assert details.getPassword() == "admin"
		assert details.getUsername() == "The Administrator"
		assert details.getAuthorities() == AuthorityUtils.createAuthorityList("ROLE_TEACHER")
		assert details.isEnabled()
	}
	
	@Test
	void user() {
		def details = service.loadUserByUsername("user")
		assert details != null
		assert details.getPassword() == "user"
		assert details.getUsername() == "A User"
		assert details.getAuthorities() == AuthorityUtils.createAuthorityList("ROLE_TEACHER")
		assert details.isEnabled()
	}
	
	@Test(expected = UsernameNotFoundException.class)
	void not_found() {
		service.loadUserByUsername("password.notfound")
	}

}