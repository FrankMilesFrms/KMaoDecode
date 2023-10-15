package psnl.frms.kmao.decode

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import psnl.frms.kmao.decode.ui.theme.KMaoDecodeTheme
import java.io.File


class MainActivity : ComponentActivity()
{
	override fun onCreate(savedInstanceState: Bundle?)
	{
		super.onCreate(savedInstanceState)
		val names = arrayOf("", "", "", "", "")
		val sharedPreferences = getSharedPreferences("frms", MODE_PRIVATE)
		names[0] = sharedPreferences.getString(SHOW_TIPS, "true")!!
		names[1] = sharedPreferences.getString(INPUT, "")!!
		names[2] = sharedPreferences.getString(OUTPUT, "")!!
		names[3] = sharedPreferences.getString(DEBUG, "false")!!
		names[4] = sharedPreferences.getString(LOW_MEMORY_MODE, "false")!!
		val editor = sharedPreferences.edit()
		
		setContent {
			KMaoDecodeTheme {
				// A surface container using the 'background' color from the theme
				Surface(
						modifier = Modifier.fillMaxSize(),
						color = MaterialTheme.colorScheme.background
				) {
					GreetingPreview(
							array = names,
							onDone =  {
								Toast.makeText(this@MainActivity, it, Toast.LENGTH_LONG).show()
							}
					) { key: String, value: String ->  
						editor.putString(key, value)
						editor.apply()
					}
				}
			}
		}
	}


	@RequiresApi(Build.VERSION_CODES.R)
	override fun onResume()
	{
		super.onResume()
		getPower()
	}
	
	
	@RequiresApi(Build.VERSION_CODES.R)
	private fun getPower()
	{
		val flag = ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
				ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
		
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S){
			if (flag) {
				Toast.makeText(this, "申请权限以读取文件", Toast.LENGTH_SHORT).show()
				ActivityCompat.requestPermissions(
						this,
						arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
								android.Manifest.permission.READ_EXTERNAL_STORAGE
						), 100)
				
				if (!Environment.isExternalStorageManager()) {
					val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
					intent.data = Uri.parse("package:$packageName")
					startActivityForResult(intent, 1024)
				}
			}
		}
		
	}
}

/**
 *
 * @param array Array<String> 初始数据，包含如下：
 * 0. 是否开启提示（"false", "true", key: "showTips"）
 * 1. 输入目标文件(key: "input")
 * 2. 输出目标文件(key: "output")
 * 3. 保留输出信息(key: "debug")
 * @param onDone Function1<String, Unit>
 * @param saveData Function2<[@kotlin.ParameterName] String, [@kotlin.ParameterName] String, Unit>
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GreetingPreview(array: Array<String>, onDone: (String) -> Unit, onSaveData:(key: String, value:String)->Unit)
{
	var showDialog by remember {
		mutableStateOf(false)
	}
	var inputFile by remember {
		mutableStateOf(array[1])
	}
	
	var outputFile by remember {
		mutableStateOf(array[2])
	}
	
	var key by remember {
		mutableStateOf("242ccb8230d709e1")
	}
	
	var aesOption by remember {
		mutableStateOf("AES/CBC/PKCS5Padding")
	}
	
	var saveFileName by remember {
		mutableStateOf(array[3] == "true")
	}
	
	var showTips by remember {
		mutableStateOf(array[0] == "true")
	}
	
	var lowMemoryMode by remember {
		mutableStateOf(array[4] == "true")
	}
	
	if(showDialog)
	{
		ResultDialog(
				input = inputFile,
				output = outputFile,
				key = key,
				option = aesOption,
				onDone = onDone,
				lowMemoryMode = lowMemoryMode,
				saveInfo = saveFileName
		) {
			showDialog = false
		}
	}
	Column(modifier = Modifier
			.fillMaxSize()
			.verticalScroll(rememberScrollState())
			.padding(
					start = 10.dp,
					end = 10.dp
			)
	){
		if(showTips) {
			Text(text = "当前版本为 1.3 版本，勉强可用，后续如果催更人数很多，再行更新。催更/检查更新 请访问（长按复制）：")
		}
		
		
		SelectionContainer {
			Text(text = "https://github.com/FrankMilesFrms/KMaoDecode")
		}
		
		Spacer(modifier = Modifier.height(15.dp))
		if(showTips)
		{
			Text(
					text = "可能存在的Bug: \n" +
							"1. 单线程读取，请不要在生成完之前结束程序，即使它可能造成短暂的系统卡顿\n" +
							"2. 由于是按照文件前缀的数字排序，不能保证一定正确，有问题请反馈。"
			)
			Spacer(modifier = Modifier.height(15.dp))
			Text(text = "警告！软件暂时不支持Android/data的读写，因此，请将小说文件夹复制到其他可访问目录下，以便于读取，小说文件夹位于：/storage/emulated/0/Android/data/com.kmxs.reader/files/KmxsReader/books/")
			Spacer(modifier = Modifier.height(15.dp))
		}
		
		Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically){
			Checkbox(
					checked = showTips,
					onCheckedChange = {
						showTips = showTips.not()
						onSaveData(SHOW_TIPS, showTips.toString())
					}
			)
			
			Text(text = (if(showTips)"关闭" else "开启" )+"所有提示")
		}
		
		Spacer(modifier = Modifier.height(15.dp))
		OutlinedTextField(
				value = inputFile,
				onValueChange = {
					inputFile = it
				},
				label = {
					Text(text = "请输入目标文件夹")
				},
				modifier = Modifier.fillMaxWidth()
		)
		
		if(showTips)
		{
			Spacer(modifier = Modifier.height(15.dp))
			Text(text = "示例：假设我想输出的文件 在根目录下，文件为'a.txt'，那么，输入的路径是以'/sdcard/a.txt'（不含单引号）。")
			
		}
		Spacer(modifier = Modifier.height(15.dp))
		OutlinedTextField(
				value = outputFile,
				onValueChange = {
					outputFile = it
				},
				label = {
					Text(text = "请输入 输出文件 完整目录")
				},
				modifier = Modifier.fillMaxWidth()
		)
		Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically){
			Checkbox(
					checked = lowMemoryMode,
					onCheckedChange = {
						lowMemoryMode = lowMemoryMode.not()
						onSaveData(DEBUG, lowMemoryMode.toString())
					}
			)
			
			Text(text = "低内存模式：适用于大文件，减低写入内存峰值，但会消耗更多时间。")
		}
		if(showTips)
		{
			Spacer(modifier = Modifier.height(15.dp))
			Text(text = "以下输入框不懂不要乱动↓")
		}
		Spacer(modifier = Modifier.height(15.dp))
		Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically){
			Checkbox(
					checked = saveFileName,
					onCheckedChange = {
						saveFileName = saveFileName.not()
						onSaveData(DEBUG, saveFileName.toString())
					}
			)
			
			Text(text = "保留输出信息在每章节正文开头（影响阅读）")
		}
		Spacer(modifier = Modifier.height(15.dp))
		OutlinedTextField(
				value = key,
				onValueChange = {
					key = it
				},
				label = {
					Text(text = "AES密匙")
				},
				modifier = Modifier.fillMaxWidth()
		)
		Spacer(modifier = Modifier.height(15.dp))
		OutlinedTextField(
				value = aesOption,
				onValueChange = {
					aesOption = it
				},
				label = {
					Text(text = "AES/工作模式/填充方式")
				},
				modifier = Modifier.fillMaxWidth()
		)
		Spacer(modifier = Modifier.height(15.dp))
		Button(
				onClick = {
					showDialog = true
					onSaveData(INPUT, inputFile)
					onSaveData(OUTPUT, outputFile)
				},
				modifier = Modifier.fillMaxWidth()
		) {
			Text(text = "保存配置并生成")
		}
		
		Spacer(modifier = Modifier.height(15.dp))
		Text(
				text = "本软件仅供学习交流使用，请勿违反您当地法律法规，否则后果自负！",
				textAlign = TextAlign.Center,
				modifier = Modifier.fillMaxWidth()
		)
		Text(
				text = "有任何好的建议请以邮件的方式与我取得联系",
				textAlign = TextAlign.Center,
				modifier = Modifier.fillMaxWidth()
		)
		SelectionContainer {
			Text(
					text = "FrankMiles@qq.com",
					textAlign = TextAlign.Center,
					modifier = Modifier.fillMaxWidth()
			)
		}
		Spacer(modifier = Modifier.height(35.dp))
	}
}

@Composable
fun ResultDialog(
		input: String,
		output: String,
		key : String,
		option:String,
		saveInfo : Boolean,
		lowMemoryMode: Boolean,
		onDone:(String) ->Unit = {},
		onClose:()->Unit= {}
) {
	val res = check(input = input, output = output)
	var click by remember {
		mutableStateOf(true)
	}
	AlertDialog(
			onDismissRequest = onClose,
			confirmButton = {
//					Text(text = "关闭")
			},
			title = {
				Text(text = "生成信息")
			},
			text = {
				Column {
					Text(text = res.first.toString())
					Button(
						onClick = {
							click = false

							onDone(
								buildNovel(File(input), File(output), key, option, saveInfo, lowMemoryMode = lowMemoryMode)
							)
						},
						modifier = Modifier.fillMaxWidth(),
						enabled = res.second and click
					) {
						Text(text = "生成")
					}
				}
			}
	)
}